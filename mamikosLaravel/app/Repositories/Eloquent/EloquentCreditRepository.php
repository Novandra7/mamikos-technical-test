<?php

declare(strict_types=1);

namespace App\Repositories\Eloquent;

use App\Models\CreditBalance;
use App\Models\CreditTransaction;
use App\Models\User;
use App\Repositories\Contracts\CreditRepositoryInterface;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;

final class EloquentCreditRepository implements CreditRepositoryInterface
{
    public function lockBalanceForUser(int $userId): ?CreditBalance
    {
        return CreditBalance::query()
            ->where('user_id', $userId)
            ->lockForUpdate()
            ->first();
    }

    public function findBalanceForUser(int $userId): ?CreditBalance
    {
        return CreditBalance::query()->where('user_id', $userId)->first();
    }

    public function createBalance(int $userId, int $balance): CreditBalance
    {
        return CreditBalance::create([
            'user_id' => $userId,
            'balance' => $balance,
            'last_recharged_at' => now(),
        ]);
    }

    public function recordTransaction(array $attributes): CreditTransaction
    {
        return CreditTransaction::create($attributes);
    }

    /**
     * @return LengthAwarePaginator<int, CreditTransaction>
     */
    public function paginateTransactions(int $userId, int $page, int $perPage): LengthAwarePaginator
    {
        return CreditTransaction::query()
            ->where('user_id', $userId)
            ->orderByDesc('created_at')
            ->paginate(perPage: $perPage, page: $page);
    }

    /**
     * @param  callable(User): void  $callback
     */
    public function chunkWalletHoldersById(int $chunkSize, callable $callback): void
    {
        User::query()
            ->whereIn('role', ['regular', 'premium'])
            ->whereHas('creditBalance')
            ->orderBy('id')
            ->chunkById($chunkSize, function ($users) use ($callback): void {
                foreach ($users as $user) {
                    $callback($user);
                }
            });
    }
}
