<?php

declare(strict_types=1);

namespace Database\Factories;

use App\Enums\RoomType;
use App\Models\Kost;
use App\Models\User;
use Illuminate\Database\Eloquent\Factories\Factory;

/**
 * @extends Factory<Kost>
 */
class KostFactory extends Factory
{
    protected $model = Kost::class;

    /**
     * Faker's `state()` provider is US-specific and not statically known
     * to PHPStan, plus it wouldn't produce a real Indonesian province
     * name anyway — pick from a fixed list instead.
     *
     * @var list<string>
     */
    private const array PROVINCES = [
        'DI Yogyakarta', 'Jawa Barat', 'Jawa Tengah', 'Jawa Timur',
        'DKI Jakarta', 'Banten', 'Bali', 'Sumatera Utara',
    ];

    public function definition(): array
    {
        $totalRooms = fake()->numberBetween(4, 30);

        return [
            'owner_id' => User::factory()->owner(),
            'name' => 'Kost '.fake()->unique()->company(),
            'description' => fake()->paragraph(),
            'address_street' => fake()->streetAddress(),
            'address_district' => fake()->citySuffix(),
            'address_city' => fake()->city(),
            'address_province' => fake()->randomElement(self::PROVINCES),
            'postal_code' => fake()->postcode(),
            'latitude' => fake()->latitude(-8, -6),
            'longitude' => fake()->longitude(106, 112),
            'price_per_month' => fake()->numberBetween(500_000, 5_000_000),
            'room_type' => fake()->randomElement(RoomType::cases()),
            'total_rooms' => $totalRooms,
            'available_rooms' => fake()->numberBetween(0, $totalRooms),
            'is_active' => true,
        ];
    }

    public function inactive(): static
    {
        return $this->state(fn (array $attributes) => ['is_active' => false]);
    }

    public function forOwner(User $owner): static
    {
        return $this->state(fn (array $attributes) => ['owner_id' => $owner->id]);
    }
}
