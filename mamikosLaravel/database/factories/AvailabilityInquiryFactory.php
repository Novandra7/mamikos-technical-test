<?php

declare(strict_types=1);

namespace Database\Factories;

use App\Enums\InquiryStatus;
use App\Models\AvailabilityInquiry;
use App\Models\Kost;
use App\Models\User;
use Illuminate\Database\Eloquent\Factories\Factory;

/**
 * @extends Factory<AvailabilityInquiry>
 */
class AvailabilityInquiryFactory extends Factory
{
    protected $model = AvailabilityInquiry::class;

    public function definition(): array
    {
        return [
            'kost_id' => Kost::factory(),
            'user_id' => User::factory()->regular(),
            'message' => fake()->sentence(),
            'credit_charged' => (int) config('credit.inquiry_cost', 5),
            'available_rooms_snapshot' => fake()->numberBetween(0, 10),
            'status' => InquiryStatus::PENDING,
        ];
    }

    public function answered(): static
    {
        return $this->state(fn (array $attributes) => [
            'status' => InquiryStatus::ANSWERED,
            'owner_reply' => fake()->sentence(),
            'replied_at' => now(),
        ]);
    }
}
