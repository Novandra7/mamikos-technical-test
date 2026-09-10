<?php

declare(strict_types=1);

namespace Database\Factories;

use App\Models\CreditBalance;
use App\Models\User;
use Illuminate\Database\Eloquent\Factories\Factory;

/**
 * @extends Factory<CreditBalance>
 */
class CreditBalanceFactory extends Factory
{
    protected $model = CreditBalance::class;

    public function definition(): array
    {
        return [
            'user_id' => User::factory()->regular(),
            'balance' => 20,
            'version' => 0,
            'last_recharged_at' => now(),
        ];
    }
}
