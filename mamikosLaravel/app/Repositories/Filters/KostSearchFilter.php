<?php

declare(strict_types=1);

namespace App\Repositories\Filters;

use App\DTOs\Kost\KostSearchCriteria;
use App\Models\Kost;
use Illuminate\Database\Eloquent\Builder;

/**
 * Composable query object for GET /kosts. Kept separate from the
 * repository so the controller/service never has to know how each filter
 * is translated into SQL (US-06).
 */
final class KostSearchFilter
{
    /**
     * @var array<string, string>
     */
    private const array SORT_COLUMN_MAP = [
        'price' => 'price_per_month',
        'created_at' => 'created_at',
        'name' => 'name',
    ];

    /**
     * @param  Builder<Kost>  $query
     * @return Builder<Kost>
     */
    public function apply(Builder $query, KostSearchCriteria $criteria): Builder
    {
        $query->publiclyVisible();

        if (filled($criteria->name)) {
            $query->where('name', 'like', '%'.$criteria->name.'%');
        }

        if (filled($criteria->location)) {
            $location = '%'.$criteria->location.'%';
            $query->where(function (Builder $q) use ($location): void {
                /** @var Builder<Kost> $q */
                $q->where('address_city', 'like', $location)
                    ->orWhere('address_district', 'like', $location)
                    ->orWhere('address_street', 'like', $location);
            });
        }

        if ($criteria->priceMin !== null) {
            $query->where('price_per_month', '>=', $criteria->priceMin);
        }

        if ($criteria->priceMax !== null) {
            $query->where('price_per_month', '<=', $criteria->priceMax);
        }

        if (filled($criteria->roomType)) {
            $query->where('room_type', $criteria->roomType);
        }

        $column = self::SORT_COLUMN_MAP[$criteria->sortBy] ?? 'created_at';
        $direction = $criteria->order === 'desc' ? 'desc' : 'asc';

        return $query->orderBy($column, $direction);
    }
}
