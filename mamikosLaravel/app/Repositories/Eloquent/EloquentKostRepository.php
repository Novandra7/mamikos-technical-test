<?php

declare(strict_types=1);

namespace App\Repositories\Eloquent;

use App\DTOs\Kost\KostSearchCriteria;
use App\Models\Kost;
use App\Repositories\Contracts\KostRepositoryInterface;
use App\Repositories\Filters\KostSearchFilter;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;

final class EloquentKostRepository implements KostRepositoryInterface
{
    public function __construct(private readonly KostSearchFilter $filter) {}

    public function findActiveOrFail(int $id): Kost
    {
        return Kost::query()->publiclyVisible()->with(['facilities', 'photos', 'owner'])->findOrFail($id);
    }

    public function findAnyOrFail(int $id): Kost
    {
        return Kost::withTrashed()->findOrFail($id);
    }

    /**
     * @return LengthAwarePaginator<int, Kost>
     */
    public function search(KostSearchCriteria $criteria): LengthAwarePaginator
    {
        $query = $this->filter->apply(
            Kost::query()->with(['facilities', 'photos', 'owner']),
            $criteria,
        );

        return $query->paginate(
            perPage: $criteria->perPage,
            page: $criteria->page,
        );
    }

    /**
     * @return LengthAwarePaginator<int, Kost>
     */
    public function paginateForOwner(int $ownerId, array $filters, int $page, int $perPage): LengthAwarePaginator
    {
        $query = Kost::query()
            ->withCount('inquiries')
            ->with(['facilities', 'photos'])
            ->where('owner_id', $ownerId);

        if (! ($filters['include_deleted'] ?? false)) {
            $query->whereNull('deleted_at');
        } else {
            $query->withTrashed();
        }

        if (array_key_exists('is_active', $filters) && $filters['is_active'] !== null) {
            $query->where('is_active', $filters['is_active']);
        }

        $sortBy = $filters['sort_by'] ?? 'created_at';
        $sortColumn = in_array($sortBy, ['price_per_month', 'created_at'], true) ? $sortBy : 'created_at';
        $query->orderBy($sortColumn, $filters['order'] ?? 'desc');

        return $query->paginate(perPage: $perPage, page: $page);
    }

    public function create(array $attributes): Kost
    {
        return Kost::create($attributes);
    }

    public function update(Kost $kost, array $attributes): Kost
    {
        $kost->update($attributes);

        return $kost->refresh();
    }

    public function delete(Kost $kost): void
    {
        $kost->delete();
    }
}
