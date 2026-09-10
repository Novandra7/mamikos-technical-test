<?php

declare(strict_types=1);

namespace App\Http\Controllers\Api\V1;

use App\DTOs\Kost\KostSearchCriteria;
use App\Http\Controllers\Controller;
use App\Http\Requests\Kost\SearchKostRequest;
use App\Http\Resources\KostDetailResource;
use App\Http\Resources\KostResource;
use App\Services\KostSearchService;
use App\Support\ApiResponse;
use Illuminate\Http\JsonResponse;

class KostSearchController extends Controller
{
    public function __construct(private readonly KostSearchService $kostSearchService) {}

    public function index(SearchKostRequest $request): JsonResponse
    {
        $criteria = KostSearchCriteria::fromArray($request->validated());

        $paginator = $this->kostSearchService->search($criteria);

        return ApiResponse::success(
            data: KostResource::collection($paginator->items()),
            message: 'Kost list retrieved',
            meta: array_merge(
                ApiResponse::paginationMeta($paginator),
                [
                    'filters_applied' => array_filter([
                        'name' => $criteria->name,
                        'location' => $criteria->location,
                        'price_min' => $criteria->priceMin,
                        'price_max' => $criteria->priceMax,
                        'room_type' => $criteria->roomType,
                    ], static fn ($value) => $value !== null),
                    'sort' => ['by' => $criteria->sortBy, 'order' => $criteria->order],
                ],
            ),
        );
    }

    public function show(int $kost): JsonResponse
    {
        $model = $this->kostSearchService->detail($kost);

        return ApiResponse::success(
            data: new KostDetailResource($model),
            message: 'Kost detail retrieved',
        );
    }
}
