<?php

declare(strict_types=1);

namespace App\Http\Controllers\Api\V1\Owner;

use App\Http\Controllers\Controller;
use App\Http\Requests\Inquiry\ReplyInquiryRequest;
use App\Http\Resources\InquiryResource;
use App\Services\AvailabilityInquiryService;
use App\Support\ApiResponse;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class OwnerInquiryController extends Controller
{
    public function __construct(private readonly AvailabilityInquiryService $inquiryService) {}

    public function index(Request $request): JsonResponse
    {
        $paginator = $this->inquiryService->forOwner(
            ownerId: $request->user()->id,
            filters: [
                'status' => $request->string('status')->toString() ?: null,
                'kost_id' => $request->integer('kost_id') ?: null,
            ],
            page: (int) $request->integer('page', 1),
            perPage: min((int) $request->integer('per_page', 10), 50),
        );

        return ApiResponse::success(
            data: InquiryResource::collection($paginator->items()),
            message: 'Inquiries retrieved',
            meta: ApiResponse::paginationMeta($paginator),
        );
    }

    public function reply(ReplyInquiryRequest $request, int $inquiry): JsonResponse
    {
        $model = $this->inquiryService->reply(
            inquiryId: $inquiry,
            ownerId: $request->user()->id,
            reply: $request->validated('reply'),
        );

        return ApiResponse::success(
            data: new InquiryResource($model),
            message: 'Inquiry answered successfully',
        );
    }
}
