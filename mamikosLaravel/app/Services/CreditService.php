<?php

declare(strict_types=1);

namespace App\Services;

use App\DTOs\Credit\CreditDeductionData;
use App\Enums\CreditTransactionType;
use App\Enums\UserRole;
use App\Exceptions\Domain\CreditWalletNotFoundException;
use App\Exceptions\Domain\InsufficientCreditException;
use App\Models\CreditBalance;
use App\Models\CreditTransaction;
use App\Repositories\Contracts\CreditRepositoryInterface;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;
use Illuminate\Support\Facades\DB;

/**
 * Owns every credit balance mutation. BR-03/BR-04: a balance can only ever
 * change inside a database transaction with a matching ledger row, and it
 * is never allowed to go negative.
 */
final class CreditService
{
    public function __construct(private readonly CreditRepositoryInterface $credits) {}

    public function quotaFor(UserRole $role): int
    {
        return match ($role) {
            UserRole::REGULAR => (int) config('credit.quota.regular'),
            UserRole::PREMIUM => (int) config('credit.quota.premium'),
            UserRole::OWNER => 0,
        };
    }

    /**
     * Grants the initial wallet balance at registration time (US-01).
     * Owners never get a wallet at all (D-02).
     */
    public function grantInitial(int $userId, UserRole $role): ?CreditBalance
    {
        if (! $role->hasCreditWallet()) {
            return null;
        }

        $quota = $this->quotaFor($role);
        $balance = $this->credits->createBalance($userId, $quota);

        $this->credits->recordTransaction([
            'user_id' => $userId,
            'type' => CreditTransactionType::INITIAL_GRANT,
            'amount' => $quota,
            'balance_before' => 0,
            'balance_after' => $quota,
            'description' => "Initial credit grant for {$role->value} role",
        ]);

        return $balance;
    }

    /**
     * Atomically deducts credit and records the matching ledger row.
     * Locks the balance row (`SELECT … FOR UPDATE`) so concurrent
     * deductions against the same wallet cannot both pass the balance
     * check (US-08 race-condition requirement).
     *
     * @throws CreditWalletNotFoundException
     * @throws InsufficientCreditException
     */
    public function deduct(CreditDeductionData $data): CreditBalance
    {
        return DB::transaction(function () use ($data): CreditBalance {
            $balance = $this->credits->lockBalanceForUser($data->userId);

            if ($balance === null) {
                throw new CreditWalletNotFoundException;
            }

            if ($balance->balance < $data->amount) {
                throw new InsufficientCreditException($data->amount, $balance->balance);
            }

            $before = $balance->balance;
            $after = $before - $data->amount;

            $balance->update(['balance' => $after]);

            $this->credits->recordTransaction([
                'user_id' => $data->userId,
                'type' => CreditTransactionType::INQUIRY_DEDUCTION,
                'amount' => -$data->amount,
                'balance_before' => $before,
                'balance_after' => $after,
                'reference_type' => $data->referenceType,
                'reference_id' => $data->referenceId,
                'description' => $data->description,
            ]);

            return $balance->refresh();
        });
    }

    /**
     * Same atomic guarantee as deduct(), but lets the caller create the
     * record the deduction is *for* (e.g. the inquiry row) in between the
     * balance update and the ledger insert, so the ledger's
     * `reference_id` can point at a row that only gets its id once it is
     * actually inserted. Everything still happens inside one transaction
     * (US-08 step 3-7).
     *
     * @template TReference
     *
     * @param  callable(): TReference  $createReference  must return an object exposing a public `id`.
     * @return array{balance: CreditBalance, reference: TReference}
     *
     * @throws CreditWalletNotFoundException
     * @throws InsufficientCreditException
     */
    public function deductWithReference(
        int $userId,
        int $amount,
        string $description,
        string $referenceType,
        callable $createReference,
    ): array {
        return DB::transaction(function () use ($userId, $amount, $description, $referenceType, $createReference): array {
            $balance = $this->credits->lockBalanceForUser($userId);

            if ($balance === null) {
                throw new CreditWalletNotFoundException;
            }

            if ($balance->balance < $amount) {
                throw new InsufficientCreditException($amount, $balance->balance);
            }

            $before = $balance->balance;
            $after = $before - $amount;

            $balance->update(['balance' => $after]);

            $reference = $createReference();

            $this->credits->recordTransaction([
                'user_id' => $userId,
                'type' => CreditTransactionType::INQUIRY_DEDUCTION,
                'amount' => -$amount,
                'balance_before' => $before,
                'balance_after' => $after,
                'reference_type' => $referenceType,
                'reference_id' => $reference->id,
                'description' => $description,
            ]);

            return ['balance' => $balance->refresh(), 'reference' => $reference];
        });
    }

    public function balanceFor(int $userId): ?CreditBalance
    {
        return $this->credits->findBalanceForUser($userId);
    }

    /**
     * @return LengthAwarePaginator<int, CreditTransaction>
     */
    public function transactionsFor(int $userId, int $page, int $perPage): LengthAwarePaginator
    {
        return $this->credits->paginateTransactions($userId, $page, $perPage);
    }
}
