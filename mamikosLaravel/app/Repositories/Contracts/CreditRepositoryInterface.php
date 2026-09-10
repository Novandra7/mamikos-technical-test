<?php

declare(strict_types=1);

namespace App\Repositories\Contracts;

use App\Models\CreditBalance;
use App\Models\CreditTransaction;
use App\Models\User;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;

interface CreditRepositoryInterface
{
    /**
     * Lock the balance row for the duration of the current transaction
     * (BR-03) so concurrent deductions cannot race each other.
     */
    public function lockBalanceForUser(int $userId): ?CreditBalance;

    public function findBalanceForUser(int $userId): ?CreditBalance;

    public function createBalance(int $userId, int $balance): CreditBalance;

    /**
     * @param  array<string, mixed>  $attributes
     */
    public function recordTransaction(array $attributes): CreditTransaction;

    /**
     * @return LengthAwarePaginator<int, CreditTransaction>
     */
    public function paginateTransactions(int $userId, int $page, int $perPage): LengthAwarePaginator;

    /**
     * @param  callable(User): void  $callback
     */
    public function chunkWalletHoldersById(int $chunkSize, callable $callback): void;
}
