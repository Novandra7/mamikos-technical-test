<?php

declare(strict_types=1);

namespace App\Services;

use App\Domain\Credit\RechargeStrategyResolver;
use App\Domain\Credit\RechargeSummary;
use App\Domain\Credit\Strategies\RechargeStrategyInterface;
use App\Enums\CreditTransactionType;
use App\Enums\RechargeStrategyType;
use App\Enums\UserRole;
use App\Models\CreditBalance;
use App\Models\User;
use App\Repositories\Contracts\CreditRepositoryInterface;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Throwable;

/**
 * Implements `php artisan credits:recharge` (US-11). Every user is
 * processed in its own transaction so a single failure never rolls back
 * the whole batch (REL-03 + resilience requirement in §11.1).
 */
final class CreditRechargeService
{
    public function __construct(private readonly CreditRepositoryInterface $credits) {}

    public function rechargeAll(
        RechargeStrategyType $strategy,
        bool $dryRun = false,
        ?int $userId = null,
        bool $force = false,
    ): RechargeSummary {
        $summary = new RechargeSummary;
        $started = microtime(true);
        $strategyImpl = RechargeStrategyResolver::resolve($strategy);
        $chunkSize = (int) config('credit.recharge_chunk_size', 1000);

        $process = function (User $user) use ($summary, $strategyImpl, $dryRun, $force): void {
            $summary->incrementProcessed();

            try {
                $this->processUser($user, $strategyImpl, $dryRun, $force, $summary);
            } catch (Throwable $exception) {
                $summary->incrementFailed();
                Log::error('credits.recharge.user_failed', [
                    'user_id' => $user->id,
                    'error' => $exception->getMessage(),
                ]);
            }
        };

        if ($userId !== null) {
            $user = User::find($userId);

            if ($user !== null) {
                $process($user);
            }
        } else {
            $this->credits->chunkWalletHoldersById($chunkSize, $process);
        }

        $summary->durationSeconds = microtime(true) - $started;

        Log::info('credits.recharge.completed', $summary->toArray());

        return $summary;
    }

    private function processUser(
        User $user,
        RechargeStrategyInterface $strategyImpl,
        bool $dryRun,
        bool $force,
        RechargeSummary $summary,
    ): void {
        DB::transaction(function () use ($user, $strategyImpl, $dryRun, $force, $summary): void {
            $balance = $this->credits->lockBalanceForUser($user->id);

            if ($balance === null) {
                $summary->incrementSkipped();

                return;
            }

            // BR-09: idempotent per calendar period.
            if (! $force && $this->alreadyRechargedThisPeriod($balance)) {
                $summary->incrementSkipped();

                return;
            }

            $quota = $this->quotaFor($user->role);
            $maxBalance = (int) config('credit.max_balance', 200);
            $before = $balance->balance;
            $after = $strategyImpl->apply($before, $quota, $maxBalance);

            if ($after === $before && ! $force) {
                $summary->incrementSkipped();

                return;
            }

            if ($dryRun) {
                $summary->incrementRecharged();

                return;
            }

            $balance->update(['balance' => $after, 'last_recharged_at' => now()]);

            $this->credits->recordTransaction([
                'user_id' => $user->id,
                'type' => CreditTransactionType::MONTHLY_RECHARGE,
                'amount' => $after - $before,
                'balance_before' => $before,
                'balance_after' => $after,
                'description' => 'Monthly credit recharge for '.now()->format('F Y'),
            ]);

            $summary->incrementRecharged();
        });
    }

    private function alreadyRechargedThisPeriod(CreditBalance $balance): bool
    {
        if ($balance->last_recharged_at === null) {
            return false;
        }

        $timezone = config('credit.timezone', 'Asia/Jakarta');

        return $balance->last_recharged_at->copy()->setTimezone($timezone)->isSameMonth(now($timezone));
    }

    private function quotaFor(UserRole $role): int
    {
        return match ($role) {
            UserRole::REGULAR => (int) config('credit.quota.regular'),
            UserRole::PREMIUM => (int) config('credit.quota.premium'),
            UserRole::OWNER => 0,
        };
    }
}
