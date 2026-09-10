<?php

declare(strict_types=1);

namespace App\DTOs\Kost;

use App\Domain\Shared\Address;
use App\Enums\RoomType;

final readonly class CreateKostData
{
    /**
     * @param  list<string>  $facilities
     * @param  list<string>  $photos
     */
    public function __construct(
        public int $ownerId,
        public string $name,
        public ?string $description,
        public Address $address,
        public ?float $latitude,
        public ?float $longitude,
        public float $pricePerMonth,
        public RoomType $roomType,
        public int $totalRooms,
        public int $availableRooms,
        public bool $isActive,
        public array $facilities,
        public array $photos,
    ) {}

    /**
     * @param  array<string, mixed>  $attributes
     */
    public static function fromArray(int $ownerId, array $attributes): self
    {
        return new self(
            ownerId: $ownerId,
            name: $attributes['name'],
            description: $attributes['description'] ?? null,
            address: Address::fromArray($attributes['address']),
            latitude: isset($attributes['latitude']) ? (float) $attributes['latitude'] : null,
            longitude: isset($attributes['longitude']) ? (float) $attributes['longitude'] : null,
            pricePerMonth: (float) $attributes['price_per_month'],
            roomType: RoomType::from($attributes['room_type']),
            totalRooms: (int) $attributes['total_rooms'],
            availableRooms: (int) $attributes['available_rooms'],
            isActive: (bool) ($attributes['is_active'] ?? true),
            facilities: $attributes['facilities'] ?? [],
            photos: $attributes['photos'] ?? [],
        );
    }
}
