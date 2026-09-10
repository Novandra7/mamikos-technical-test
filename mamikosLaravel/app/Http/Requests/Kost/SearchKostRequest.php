<?php

declare(strict_types=1);

namespace App\Http\Requests\Kost;

use App\DTOs\Kost\KostSearchCriteria;
use App\Enums\RoomType;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class SearchKostRequest extends FormRequest
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
            'name' => ['sometimes', 'string', 'max:150'],
            'location' => ['sometimes', 'string', 'max:150'],
            'price_min' => ['sometimes', 'numeric', 'min:0'],
            'price_max' => ['sometimes', 'numeric', 'min:0', 'gte:price_min'],
            'room_type' => ['sometimes', Rule::in(RoomType::values())],
            // Whitelisted here — this is what prevents a client from
            // sorting by an arbitrary/unindexed column (US-06, SEC-07).
            'sort_by' => ['sometimes', Rule::in(KostSearchCriteria::SORTABLE_COLUMNS)],
            'order' => ['sometimes', Rule::in(['asc', 'desc'])],
            'page' => ['sometimes', 'integer', 'min:1'],
            'per_page' => ['sometimes', 'integer', 'min:1', 'max:50'],
        ];
    }
}
