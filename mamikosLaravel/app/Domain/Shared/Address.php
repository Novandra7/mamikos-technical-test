<?php

declare(strict_types=1);

namespace App\Domain\Shared;

final readonly class Address
{
    public function __construct(
        public string $street,
        public string $district,
        public string $city,
        public string $province,
        public ?string $postalCode = null,
    ) {}

    /**
     * @param  array{street: string, district: string, city: string, province: string, postal_code?: string|null}  $attributes
     */
    public static function fromArray(array $attributes): self
    {
        return new self(
            street: $attributes['street'],
            district: $attributes['district'],
            city: $attributes['city'],
            province: $attributes['province'],
            postalCode: $attributes['postal_code'] ?? null,
        );
    }

    /**
     * @return array{street: string, district: string, city: string, province: string, postal_code: string|null}
     */
    public function toArray(): array
    {
        return [
            'street' => $this->street,
            'district' => $this->district,
            'city' => $this->city,
            'province' => $this->province,
            'postal_code' => $this->postalCode,
        ];
    }
}
