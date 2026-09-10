<?php

declare(strict_types=1);

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Http\Resources\CreditBalanceResource;
use App\Http\Resources\CreditTransactionResource;
use App\Services\CreditService;
use App\Support\ApiResponse;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class CreditController extends Controller
{
    public function __construct(private readonly CreditService $creditService) {}

    public function balance(Request $request): JsonResponse
    {
        $balance = $this->creditService->balanceFor($request->user()->id);
        $balance?->setRelation('user', $request->user());

        return ApiResponse::success(
            data: new CreditBalanceResource($balance),
            message: 'Credit balance retrieved',
        );
    }

    public function transactions(Request $request): JsonResponse
    {
        $paginator = $this->creditService->transactionsFor(
            userId: $request->user()->id,
            page: (int) $request->integer('page', 1),
            perPage: min((int) $request->integer('per_page', 10), 50),
        );

        return ApiResponse::success(
            data: CreditTransactionResource::collection($paginator->items()),
            message: 'Credit transactions retrieved',
            meta: ApiResponse::paginationMeta($paginator),
        );
    }
}
