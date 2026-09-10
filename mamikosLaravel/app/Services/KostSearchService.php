<?php

declare(strict_types=1);

namespace App\Services;

use App\DTOs\Kost\KostSearchCriteria;
use App\Models\Kost;
use App\Repositories\Contracts\KostRepositoryInterface;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;

/**
 * Public, unauthenticated search and detail (US-06, US-07). Never exposes
 * `available_rooms` — that is only revealed through a paid inquiry (D-03,
 * BR-11).
 */
final class KostSearchService
{
    public function __construct(private readonly KostRepositoryInterface $kosts) {}

    /**
     * @return LengthAwarePaginator<int, Kost>
     */
    public function search(KostSearchCriteria $criteria): LengthAwarePaginator
    {
        return $this->kosts->search($criteria);
    }

    public function detail(int $kostId): Kost
    {
        return $this->kosts->findActiveOrFail($kostId);
    }
}
