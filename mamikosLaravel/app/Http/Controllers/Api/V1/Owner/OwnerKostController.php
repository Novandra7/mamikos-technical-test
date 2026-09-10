<?php

declare(strict_types=1);

namespace App\Http\Controllers\Api\V1\Owner;

use App\DTOs\Kost\CreateKostData;
use App\DTOs\Kost\UpdateKostData;
use App\Http\Controllers\Controller;
use App\Http\Requests\Kost\StoreKostRequest;
use App\Http\Requests\Kost\UpdateKostRequest;
use App\Http\Resources\OwnerKostResource;
use App\Services\KostService;
use App\Support\ApiResponse;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class OwnerKostController extends Controller
{
    public function __construct(private readonly KostService $kostService) {}

    public function store(StoreKostRequest $request): JsonResponse
    {
        $data = CreateKostData::fromArray($request->user()->id, $request->validated());

        $kost = $this->kostService->create($data);

        return ApiResponse::success(
            data: new OwnerKostResource($kost),
            message: 'Kost created successfully',
            status: 201,
        );
    }

    public function index(Request $request): JsonResponse
    {
        $page = (int) $request->integer('page', 1);
        $perPage = min((int) $request->integer('per_page', 10), 50);

        $paginator = $this->kostService->listForOwner(
            ownerId: $request->user()->id,
            filters: [
                'is_active' => $request->has('is_active') ? $request->boolean('is_active') : null,
                'include_deleted' => $request->boolean('include_deleted'),
                'sort_by' => $request->string('sort_by')->toString() ?: null,
                'order' => $request->string('order')->toString() ?: 'desc',
            ],
            page: $page,
            perPage: $perPage,
        );

        return ApiResponse::success(
            data: OwnerKostResource::collection($paginator->items()),
            message: 'Owner kost list retrieved',
            meta: ApiResponse::paginationMeta($paginator),
        );
    }

    public function show(Request $request, int $kost): JsonResponse
    {
        $model = $this->kostService->findOwned($kost, $request->user()->id);

        return ApiResponse::success(
            data: new OwnerKostResource($model),
            message: 'Kost detail retrieved',
        );
    }

    public function update(UpdateKostRequest $request, int $kost): JsonResponse
    {
        $data = UpdateKostData::fromArray($request->validated());

        $model = $this->kostService->update($kost, $request->user()->id, $data);

        return ApiResponse::success(
            data: new OwnerKostResource($model),
            message: 'Kost updated successfully',
        );
    }

    public function destroy(Request $request, int $kost): JsonResponse
    {
        $this->kostService->delete($kost, $request->user()->id);

        return ApiResponse::success(message: 'Kost deleted successfully');
    }
}
