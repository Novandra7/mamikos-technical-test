<?php

declare(strict_types=1);

namespace App\Repositories\Contracts;

use App\DTOs\Kost\KostSearchCriteria;
use App\Models\Kost;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;

interface KostRepositoryInterface
{
    /**
     * A kost visible on the public search/detail endpoints: active and not
     * soft-deleted (BR-07). Throws ModelNotFoundException otherwise.
     */
    public function findActiveOrFail(int $id): Kost;

    /**
     * A kost as seen by its owner (may be inactive or soft-deleted).
     * Throws ModelNotFoundException if the id does not exist at all.
     */
    public function findAnyOrFail(int $id): Kost;

    /**
     * @return LengthAwarePaginator<int, Kost>
     */
    public function search(KostSearchCriteria $criteria): LengthAwarePaginator;

    /**
     * @param  array<string, mixed>  $filters
     * @return LengthAwarePaginator<int, Kost>
     */
    public function paginateForOwner(int $ownerId, array $filters, int $page, int $perPage): LengthAwarePaginator;

    /**
     * @param  array<string, mixed>  $attributes
     */
    public function create(array $attributes): Kost;

    /**
     * @param  array<string, mixed>  $attributes
     */
    public function update(Kost $kost, array $attributes): Kost;

    public function delete(Kost $kost): void;
}
