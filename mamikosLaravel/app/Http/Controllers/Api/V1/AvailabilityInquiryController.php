<?php

declare(strict_types=1);

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Http\Requests\Inquiry\StoreInquiryRequest;
use App\Http\Resources\InquiryResource;
use App\Services\AvailabilityInquiryService;
use App\Support\ApiResponse;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class AvailabilityInquiryController extends Controller
{
    public function __construct(private readonly AvailabilityInquiryService $inquiryService) {}

    public function store(StoreInquiryRequest $request, int $kost): JsonResponse
    {
        $result = $this->inquiryService->create(
            kostId: $kost,
            userId: $request->user()->id,
            message: $request->validated('message'),
        );

        $inquiry = $result['inquiry'];

        return ApiResponse::success(
            data: [
                'inquiry' => new InquiryResource($inquiry),
                'availability' => [
                    'disclosed' => true,
                    'available_rooms' => $inquiry->available_rooms_snapshot,
                    'total_rooms' => $inquiry->kost->total_rooms,
                    'is_available' => $inquiry->available_rooms_snapshot > 0,
                    'as_of' => $inquiry->created_at?->toIso8601String(),
                ],
                'credit' => [
                    'charged' => $result['charged'],
                    'balance_before' => $result['balance_before'],
                    'balance_after' => $result['balance_after'],
                ],
            ],
            message: 'Availability inquiry submitted',
            status: 201,
        );
    }

    public function myInquiries(Request $request): JsonResponse
    {
        $paginator = $this->inquiryService->myInquiries(
            userId: $request->user()->id,
            page: (int) $request->integer('page', 1),
            perPage: min((int) $request->integer('per_page', 10), 50),
        );

        return ApiResponse::success(
            data: InquiryResource::collection($paginator->items()),
            message: 'Inquiry history retrieved',
            meta: ApiResponse::paginationMeta($paginator),
        );
    }
}
