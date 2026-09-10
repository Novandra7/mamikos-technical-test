<?php

declare(strict_types=1);

namespace Tests\Concerns;

use App\Models\CreditBalance;
use App\Models\User;

trait CreatesUsers
{
    protected function createOwner(array $attributes = []): User
    {
        return User::factory()->owner()->create($attributes);
    }

    protected function createRegular(int $balance = 20, array $attributes = []): User
    {
        $user = User::factory()->regular()->create($attributes);
        CreditBalance::factory()->for($user, 'user')->create(['balance' => $balance]);

        return $user->fresh();
    }

    protected function createPremium(int $balance = 40, array $attributes = []): User
    {
        $user = User::factory()->premium()->create($attributes);
        CreditBalance::factory()->for($user, 'user')->create(['balance' => $balance]);

        return $user->fresh();
    }
}
