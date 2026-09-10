<?php

declare(strict_types=1);

namespace App\Repositories\Contracts;

use App\Models\AvailabilityInquiry;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;

interface InquiryRepositoryInterface
{
    /**
     * @param  array<string, mixed>  $attributes
     */
    public function create(array $attributes): AvailabilityInquiry;

    public function findOrFail(int $id): AvailabilityInquiry;

    /**
     * @param  array<string, mixed>  $filters
     * @return LengthAwarePaginator<int, AvailabilityInquiry>
     */
    public function paginateForOwner(int $ownerId, array $filters, int $page, int $perPage): LengthAwarePaginator;

    /**
     * @return LengthAwarePaginator<int, AvailabilityInquiry>
     */
    public function paginateForUser(int $userId, int $page, int $perPage): LengthAwarePaginator;
}
