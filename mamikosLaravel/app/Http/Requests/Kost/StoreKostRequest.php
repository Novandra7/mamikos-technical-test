<?php

declare(strict_types=1);

namespace App\Http\Requests\Kost;

use App\Enums\RoomType;
use App\Rules\AvailableRoomsWithinCapacity;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

/**
 * `owner_id` is deliberately absent from these rules: it always comes from
 * the authenticated user, never the request body (US-03, SEC-06).
 */
class StoreKostRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /**
     * @return array<string, mixed>
     */
    public function rules(): array
    {
        return [
            'name' => ['required', 'string', 'max:150'],
            'description' => ['nullable', 'string'],
            'address' => ['required', 'array'],
            'address.street' => ['required', 'string', 'max:255'],
            'address.district' => ['required', 'string', 'max:100'],
            'address.city' => ['required', 'string', 'max:100'],
            'address.province' => ['required', 'string', 'max:100'],
            'address.postal_code' => ['nullable', 'string', 'max:10'],
            'latitude' => ['nullable', 'numeric', 'between:-90,90'],
            'longitude' => ['nullable', 'numeric', 'between:-180,180'],
            'price_per_month' => ['required', 'numeric', 'min:0.01', 'max:999999999.99'],
            'room_type' => ['required', Rule::in(RoomType::values())],
            'total_rooms' => ['required', 'integer', 'min:1'],
            'available_rooms' => [
                'required', 'integer', 'min:0',
                new AvailableRoomsWithinCapacity($this->input('total_rooms')),
            ],
            'is_active' => ['sometimes', 'boolean'],
            'facilities' => ['sometimes', 'array', 'max:20'],
            'facilities.*' => ['string', 'max:50'],
            'photos' => ['sometimes', 'array', 'max:10'],
            'photos.*' => ['url'],
        ];
    }
}
