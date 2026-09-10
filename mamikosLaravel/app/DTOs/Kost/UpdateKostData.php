<?php

declare(strict_types=1);

namespace App\DTOs\Kost;

use App\Domain\Shared\Address;
use App\Enums\RoomType;

/**
 * All fields are optional so the same DTO serves both PUT (full payload)
 * and PATCH (partial payload) — only the properties present in the request
 * are ever set to a non-null value.
 */
final readonly class UpdateKostData
{
    /**
     * @param  list<string>|null  $facilities
     * @param  list<string>|null  $photos
     */
    public function __construct(
        public ?string $name = null,
        public ?string $description = null,
        public ?Address $address = null,
        public ?float $latitude = null,
        public ?float $longitude = null,
        public ?float $pricePerMonth = null,
        public ?RoomType $roomType = null,
        public ?int $totalRooms = null,
        public ?int $availableRooms = null,
        public ?bool $isActive = null,
        public ?array $facilities = null,
        public ?array $photos = null,
    ) {}

    /**
     * @param  array<string, mixed>  $attributes
     */
    public static function fromArray(array $attributes): self
    {
        return new self(
            name: $attributes['name'] ?? null,
            description: $attributes['description'] ?? null,
            address: isset($attributes['address']) ? Address::fromArray($attributes['address']) : null,
            latitude: isset($attributes['latitude']) ? (float) $attributes['latitude'] : null,
            longitude: isset($attributes['longitude']) ? (float) $attributes['longitude'] : null,
            pricePerMonth: isset($attributes['price_per_month']) ? (float) $attributes['price_per_month'] : null,
            roomType: isset($attributes['room_type']) ? RoomType::from($attributes['room_type']) : null,
            totalRooms: isset($attributes['total_rooms']) ? (int) $attributes['total_rooms'] : null,
            availableRooms: isset($attributes['available_rooms']) ? (int) $attributes['available_rooms'] : null,
            isActive: array_key_exists('is_active', $attributes) ? (bool) $attributes['is_active'] : null,
            facilities: $attributes['facilities'] ?? null,
            photos: $attributes['photos'] ?? null,
        );
    }

    /**
     * @return array<string, mixed>
     */
    public function toKostAttributes(): array
    {
        $address = $this->address?->toArray();

        return array_filter([
            'name' => $this->name,
            'description' => $this->description,
            'address_street' => $address['street'] ?? null,
            'address_district' => $address['district'] ?? null,
            'address_city' => $address['city'] ?? null,
            'address_province' => $address['province'] ?? null,
            'postal_code' => $address['postal_code'] ?? null,
            'latitude' => $this->latitude,
            'longitude' => $this->longitude,
            'price_per_month' => $this->pricePerMonth,
            'room_type' => $this->roomType?->value,
            'total_rooms' => $this->totalRooms,
            'available_rooms' => $this->availableRooms,
            'is_active' => $this->isActive,
        ], static fn ($value) => $value !== null);
    }
}
