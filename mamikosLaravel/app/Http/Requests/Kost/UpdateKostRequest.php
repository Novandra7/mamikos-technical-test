<?php

declare(strict_types=1);

namespace App\Http\Requests\Kost;

use App\Enums\RoomType;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

/**
 * Backs both PUT (full replace) and PATCH (partial update). Every field is
 * "sometimes" — the difference between the two verbs is left to the
 * client's payload shape rather than to divergent rule sets, which keeps
 * one request class instead of two near-duplicates. The
 * available-rooms-within-capacity invariant is enforced in KostService,
 * where the existing record is available to fall back on for whichever
 * side of the comparison the client did not send (BR-12).
 */
class UpdateKostRequest extends FormRequest
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
            'name' => ['sometimes', 'required', 'string', 'max:150'],
            'description' => ['sometimes', 'nullable', 'string'],
            'address' => ['sometimes', 'required', 'array'],
            'address.street' => ['required_with:address', 'string', 'max:255'],
            'address.district' => ['required_with:address', 'string', 'max:100'],
            'address.city' => ['required_with:address', 'string', 'max:100'],
            'address.province' => ['required_with:address', 'string', 'max:100'],
            'address.postal_code' => ['nullable', 'string', 'max:10'],
            'latitude' => ['sometimes', 'nullable', 'numeric', 'between:-90,90'],
            'longitude' => ['sometimes', 'nullable', 'numeric', 'between:-180,180'],
            'price_per_month' => ['sometimes', 'required', 'numeric', 'min:0.01', 'max:999999999.99'],
            'room_type' => ['sometimes', 'required', Rule::in(RoomType::values())],
            'total_rooms' => ['sometimes', 'required', 'integer', 'min:1'],
            'available_rooms' => ['sometimes', 'required', 'integer', 'min:0'],
            'is_active' => ['sometimes', 'boolean'],
            'facilities' => ['sometimes', 'array', 'max:20'],
            'facilities.*' => ['string', 'max:50'],
            'photos' => ['sometimes', 'array', 'max:10'],
            'photos.*' => ['url'],
        ];
    }
}
