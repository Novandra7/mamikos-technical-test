<?php

declare(strict_types=1);

namespace App\DTOs\Kost;

final readonly class KostSearchCriteria
{
    /**
     * Sort columns are whitelisted here and nowhere else — this is what
     * prevents a client from sorting by an arbitrary column (US-06, SEC-07).
     *
     * @var list<string>
     */
    public const array SORTABLE_COLUMNS = ['price', 'created_at', 'name'];

    public function __construct(
        public ?string $name = null,
        public ?string $location = null,
        public ?float $priceMin = null,
        public ?float $priceMax = null,
        public ?string $roomType = null,
        public string $sortBy = 'created_at',
        public string $order = 'asc',
        public int $page = 1,
        public int $perPage = 10,
    ) {}

    /**
     * @param  array<string, mixed>  $query
     */
    public static function fromArray(array $query): self
    {
        return new self(
            name: $query['name'] ?? null,
            location: $query['location'] ?? null,
            priceMin: isset($query['price_min']) ? (float) $query['price_min'] : null,
            priceMax: isset($query['price_max']) ? (float) $query['price_max'] : null,
            roomType: $query['room_type'] ?? null,
            sortBy: $query['sort_by'] ?? 'created_at',
            order: $query['order'] ?? 'asc',
            page: isset($query['page']) ? (int) $query['page'] : 1,
            perPage: isset($query['per_page']) ? (int) $query['per_page'] : 10,
        );
    }
}
