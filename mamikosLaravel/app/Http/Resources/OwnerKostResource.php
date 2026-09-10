<?php

declare(strict_types=1);

namespace App\Http\Resources;

use App\Models\Kost;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * The owner's own view of a kost — unlike KostResource this includes
 * `available_rooms`, `is_active`, and moderation metadata (US-04/US-05).
 *
 * @mixin Kost
 */
class OwnerKostResource extends JsonResource
{
    /**
     * @return array<string, mixed>
     */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'name' => $this->name,
            'description' => $this->description,
            'address' => [
                'street' => $this->address_street,
                'district' => $this->address_district,
                'city' => $this->address_city,
                'province' => $this->address_province,
                'postal_code' => $this->postal_code,
            ],
            'latitude' => $this->latitude,
            'longitude' => $this->longitude,
            'price_per_month' => $this->price_per_month,
            'room_type' => $this->room_type->value,
            'total_rooms' => $this->total_rooms,
            'available_rooms' => $this->available_rooms,
            'is_active' => $this->is_active,
            'facilities' => $this->facilities->pluck('name')->values(),
            'photos' => $this->photos->pluck('url')->values(),
            'inquiries_count' => $this->whenCounted('inquiries'),
            'created_at' => $this->created_at?->toIso8601String(),
            'updated_at' => $this->updated_at?->toIso8601String(),
            'deleted_at' => $this->deleted_at?->toIso8601String(),
        ];
    }
}
