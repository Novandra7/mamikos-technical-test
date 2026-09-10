<?php

declare(strict_types=1);

namespace App\Repositories\Eloquent;

use App\Models\AvailabilityInquiry;
use App\Repositories\Contracts\InquiryRepositoryInterface;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;

final class EloquentInquiryRepository implements InquiryRepositoryInterface
{
    public function create(array $attributes): AvailabilityInquiry
    {
        return AvailabilityInquiry::create($attributes);
    }

    public function findOrFail(int $id): AvailabilityInquiry
    {
        return AvailabilityInquiry::query()->with(['kost', 'user'])->findOrFail($id);
    }

    /**
     * @return LengthAwarePaginator<int, AvailabilityInquiry>
     */
    public function paginateForOwner(int $ownerId, array $filters, int $page, int $perPage): LengthAwarePaginator
    {
        $query = AvailabilityInquiry::query()
            ->with(['kost', 'user'])
            ->whereHas('kost', fn ($q) => $q->where('owner_id', $ownerId));

        if (filled($filters['status'] ?? null)) {
            $query->where('status', $filters['status']);
        }

        if (filled($filters['kost_id'] ?? null)) {
            $query->where('kost_id', $filters['kost_id']);
        }

        return $query->orderByDesc('created_at')->paginate(perPage: $perPage, page: $page);
    }

    /**
     * @return LengthAwarePaginator<int, AvailabilityInquiry>
     */
    public function paginateForUser(int $userId, int $page, int $perPage): LengthAwarePaginator
    {
        return AvailabilityInquiry::query()
            ->with('kost')
            ->where('user_id', $userId)
            ->orderByDesc('created_at')
            ->paginate(perPage: $perPage, page: $page);
    }
}
