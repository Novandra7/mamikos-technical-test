<?php

declare(strict_types=1);

namespace Database\Seeders;

use App\Enums\RoomType;
use App\Models\Kost;
use App\Models\User;
use Illuminate\Database\Seeder;

/**
 * Seeds 30 kosts across 5 cities with a wide price spread, so the search
 * filter/sort/pagination behaviour (US-06) has something real to exercise.
 */
class KostSeeder extends Seeder
{
    private const array CITIES = [
        ['city' => 'Sleman', 'province' => 'DI Yogyakarta', 'district' => 'Depok'],
        ['city' => 'Bandung', 'province' => 'Jawa Barat', 'district' => 'Coblong'],
        ['city' => 'Surabaya', 'province' => 'Jawa Timur', 'district' => 'Gubeng'],
        ['city' => 'Malang', 'province' => 'Jawa Timur', 'district' => 'Lowokwaru'],
        ['city' => 'Depok', 'province' => 'Jawa Barat', 'district' => 'Beji'],
        ['city' => 'Jakarta Selatan', 'province' => 'DKI Jakarta', 'district' => 'Kebayoran Baru'],
    ];

    private const array FACILITY_POOL = [
        'wifi', 'kamar mandi dalam', 'parkir motor', 'ac', 'kasur', 'lemari',
        'dapur bersama', 'laundry', 'cctv', 'meja belajar',
    ];

    public function run(): void
    {
        $owners = User::query()
            ->whereIn('email', ['owner1@mamikos.test', 'owner2@mamikos.test'])
            ->get();

        if ($owners->isEmpty()) {
            $owners = User::factory()->owner()->count(2)->create();
        }

        for ($i = 1; $i <= 30; $i++) {
            $location = self::CITIES[$i % count(self::CITIES)];
            $totalRooms = fake()->numberBetween(4, 25);
            $price = fake()->numberBetween(500_000, 5_000_000);

            $kost = Kost::create([
                'owner_id' => $owners->get($i % $owners->count())->id,
                'name' => 'Kost '.fake()->unique()->streetName().' No. '.$i,
                'description' => fake()->paragraph(),
                'address_street' => fake()->streetAddress(),
                'address_district' => $location['district'],
                'address_city' => $location['city'],
                'address_province' => $location['province'],
                'postal_code' => fake()->postcode(),
                'latitude' => fake()->latitude(-8, -6),
                'longitude' => fake()->longitude(106, 114),
                'price_per_month' => $price,
                'room_type' => fake()->randomElement(RoomType::cases()),
                'total_rooms' => $totalRooms,
                'available_rooms' => fake()->numberBetween(0, $totalRooms),
                'is_active' => true,
            ]);

            foreach (fake()->randomElements(self::FACILITY_POOL, fake()->numberBetween(3, 6)) as $facility) {
                $kost->facilities()->create(['name' => $facility]);
            }

            foreach (range(1, fake()->numberBetween(1, 3)) as $order) {
                $kost->photos()->create([
                    'url' => "https://cdn.example.test/kost/{$kost->id}/{$order}.jpg",
                    'sort_order' => $order - 1,
                ]);
            }
        }
    }
}
