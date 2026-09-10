<?php

declare(strict_types=1);

namespace App\Http\Resources;

use App\Models\Kost;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * Public detail (GET /kosts/{id}). BR-14: the owner's phone number is only
 * revealed to authenticated users, never to a guest.
 *
 * @mixin Kost
 */
class KostDetailResource extends JsonResource
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
            'facilities' => $this->facilities->pluck('name')->values(),
            'photos' => $this->photos->pluck('url')->values(),
            'owner' => [
                'id' => $this->owner->id,
                'name' => $this->owner->name,
                'phone' => $request->user() ? $this->owner->phone : null,
            ],
            'availability' => [
                'disclosed' => false,
                'hint' => 'Ask the owner to reveal room availability',
            ],
            'created_at' => $this->created_at?->toIso8601String(),
        ];
    }
}
