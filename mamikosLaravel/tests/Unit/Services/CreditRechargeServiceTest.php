<?php

declare(strict_types=1);

namespace Tests\Unit\Services;

use App\Enums\RechargeStrategyType;
use App\Models\CreditBalance;
use App\Models\CreditTransaction;
use App\Models\User;
use App\Repositories\Contracts\CreditRepositoryInterface;
use App\Services\CreditRechargeService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Mockery;
use RuntimeException;
use Tests\Concerns\CreatesUsers;
use Tests\TestCase;

class CreditRechargeServiceTest extends TestCase
{
    use CreatesUsers, RefreshDatabase;

    private CreditRechargeService $service;

    protected function setUp(): void
    {
        parent::setUp();

        $this->service = app(CreditRechargeService::class);
    }

    public function test_reset_strategy_recharges_low_balance_to_full_quota(): void
    {
        $user = $this->createRegular(balance: 3);
        $this->makeEligibleForRecharge($user);

        $summary = $this->service->rechargeAll(RechargeStrategyType::RESET);

        $this->assertSame(20, $user->creditBalance->fresh()->balance);
        $this->assertSame(1, $summary->recharged);
        $this->assertDatabaseHas('credit_transactions', [
            'user_id' => $user->id,
            'amount' => 17,
            'type' => 'MONTHLY_RECHARGE',
        ]);
    }

    public function test_reset_strategy_skips_user_already_at_quota(): void
    {
        $user = $this->createRegular(balance: 20);
        $this->makeEligibleForRecharge($user);

        $summary = $this->service->rechargeAll(RechargeStrategyType::RESET);

        $this->assertSame(20, $user->creditBalance->fresh()->balance);
        $this->assertSame(1, $summary->skipped);
        $this->assertSame(0, $summary->recharged);
        $this->assertDatabaseCount('credit_transactions', 0);
    }

    public function test_topup_strategy_adds_quota_on_top_of_balance(): void
    {
        $user = $this->createRegular(balance: 15);
        $this->makeEligibleForRecharge($user);

        $this->service->rechargeAll(RechargeStrategyType::TOPUP);

        $this->assertSame(35, $user->creditBalance->fresh()->balance);
    }

    public function test_topup_strategy_is_capped_at_max_balance(): void
    {
        $user = $this->createRegular(balance: 195);
        $this->makeEligibleForRecharge($user);

        $this->service->rechargeAll(RechargeStrategyType::TOPUP);

        $this->assertSame((int) config('credit.max_balance'), $user->creditBalance->fresh()->balance);
    }

    public function test_owners_are_never_processed(): void
    {
        $owner = $this->createOwner();

        $summary = $this->service->rechargeAll(RechargeStrategyType::RESET);

        $this->assertSame(0, $summary->processed);
        $this->assertDatabaseCount('credit_balances', 0);
    }

    public function test_already_recharged_this_period_is_skipped(): void
    {
        $user = $this->createRegular(balance: 3);
        $user->creditBalance->update(['last_recharged_at' => now()]);

        $summary = $this->service->rechargeAll(RechargeStrategyType::RESET);

        $this->assertSame(1, $summary->skipped);
        $this->assertSame(3, $user->creditBalance->fresh()->balance);
    }

    public function test_dry_run_computes_summary_without_persisting_changes(): void
    {
        $user = $this->createRegular(balance: 3);
        $this->makeEligibleForRecharge($user);

        $summary = $this->service->rechargeAll(RechargeStrategyType::RESET, dryRun: true);

        $this->assertSame(1, $summary->recharged);
        $this->assertSame(3, $user->creditBalance->fresh()->balance);
        $this->assertDatabaseCount('credit_transactions', 0);
    }

    public function test_one_user_failure_does_not_stop_the_batch(): void
    {
        $goodUser = $this->createRegular(balance: 3);
        $badUser = $this->createRegular(balance: 3);
        $this->makeEligibleForRecharge($goodUser);
        $this->makeEligibleForRecharge($badUser);

        $repository = Mockery::mock(CreditRepositoryInterface::class);
        $repository->shouldReceive('chunkWalletHoldersById')
            ->once()
            ->andReturnUsing(function (int $chunkSize, callable $callback) use ($goodUser, $badUser): void {
                $callback($goodUser);
                $callback($badUser);
            });
        $repository->shouldReceive('lockBalanceForUser')
            ->andReturnUsing(fn (int $userId) => CreditBalance::where('user_id', $userId)->first());
        $repository->shouldReceive('recordTransaction')
            ->andReturnUsing(function (array $attributes) use ($badUser): CreditTransaction {
                if ($attributes['user_id'] === $badUser->id) {
                    throw new RuntimeException('Simulated failure');
                }

                return CreditTransaction::create($attributes);
            });

        $this->app->instance(CreditRepositoryInterface::class, $repository);
        $service = $this->app->make(CreditRechargeService::class);

        $summary = $service->rechargeAll(RechargeStrategyType::RESET);

        $this->assertSame(2, $summary->processed);
        $this->assertSame(1, $summary->recharged);
        $this->assertSame(1, $summary->failed);
        $this->assertSame(20, $goodUser->creditBalance->fresh()->balance);
    }

    private function makeEligibleForRecharge(User $user): void
    {
        // Not idempotency-skipped: pretend the wallet was last touched
        // last month rather than "at registration, this month".
        $user->creditBalance->update(['last_recharged_at' => now()->subMonthNoOverflow()]);
    }
}
