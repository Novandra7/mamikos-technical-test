<?php

declare(strict_types=1);

namespace Tests\Unit\Services;

use App\DTOs\Credit\CreditDeductionData;
use App\Enums\CreditTransactionType;
use App\Enums\UserRole;
use App\Exceptions\Domain\CreditWalletNotFoundException;
use App\Exceptions\Domain\InsufficientCreditException;
use App\Models\CreditTransaction;
use App\Models\User;
use App\Services\CreditService;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\Concerns\CreatesUsers;
use Tests\TestCase;

class CreditServiceTest extends TestCase
{
    use CreatesUsers, RefreshDatabase;

    private CreditService $creditService;

    protected function setUp(): void
    {
        parent::setUp();

        $this->creditService = app(CreditService::class);
    }

    public function test_deduct_five_from_balance_of_twenty(): void
    {
        $user = $this->createRegular(balance: 20);

        $balance = $this->creditService->deduct(new CreditDeductionData(
            userId: $user->id,
            amount: 5,
            description: 'test deduction',
        ));

        $this->assertSame(15, $balance->balance);
        $this->assertDatabaseCount('credit_transactions', 1);
        $this->assertDatabaseHas('credit_transactions', [
            'user_id' => $user->id,
            'type' => CreditTransactionType::INQUIRY_DEDUCTION->value,
            'amount' => -5,
            'balance_before' => 20,
            'balance_after' => 15,
        ]);
    }

    public function test_deduct_five_from_balance_of_three_throws_and_leaves_balance_untouched(): void
    {
        $user = $this->createRegular(balance: 3);

        $this->expectException(InsufficientCreditException::class);

        try {
            $this->creditService->deduct(new CreditDeductionData(userId: $user->id, amount: 5, description: 'test'));
        } finally {
            $this->assertSame(3, $user->creditBalance->fresh()->balance);
            $this->assertDatabaseCount('credit_transactions', 0);
        }
    }

    public function test_deduct_five_from_balance_of_exactly_five_succeeds_to_zero(): void
    {
        $user = $this->createRegular(balance: 5);

        $balance = $this->creditService->deduct(new CreditDeductionData(userId: $user->id, amount: 5, description: 'test'));

        $this->assertSame(0, $balance->balance);
    }

    public function test_deduct_for_user_without_wallet_throws(): void
    {
        $owner = $this->createOwner();

        $this->expectException(CreditWalletNotFoundException::class);

        $this->creditService->deduct(new CreditDeductionData(userId: $owner->id, amount: 5, description: 'test'));
    }

    public function test_grant_initial_for_regular_role(): void
    {
        $user = User::factory()->regular()->create();

        $balance = $this->creditService->grantInitial($user->id, UserRole::REGULAR);

        $this->assertSame(20, $balance->balance);
        $this->assertDatabaseHas('credit_transactions', [
            'user_id' => $user->id,
            'type' => CreditTransactionType::INITIAL_GRANT->value,
            'amount' => 20,
        ]);
    }

    public function test_grant_initial_for_premium_role(): void
    {
        $user = User::factory()->premium()->create();

        $balance = $this->creditService->grantInitial($user->id, UserRole::PREMIUM);

        $this->assertSame(40, $balance->balance);
    }

    public function test_grant_initial_for_owner_role_creates_no_wallet(): void
    {
        $user = $this->createOwner();

        $balance = $this->creditService->grantInitial($user->id, UserRole::OWNER);

        $this->assertNull($balance);
        $this->assertDatabaseCount('credit_balances', 0);
    }

    public function test_ledger_balance_after_is_always_balance_before_plus_amount(): void
    {
        $user = $this->createRegular(balance: 20);

        $this->creditService->deduct(new CreditDeductionData(userId: $user->id, amount: 5, description: 'test'));

        $transaction = CreditTransaction::where('user_id', $user->id)->firstOrFail();

        $this->assertSame($transaction->balance_after, $transaction->balance_before + $transaction->amount);
    }
}
