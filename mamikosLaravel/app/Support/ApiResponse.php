<?php

declare(strict_types=1);

namespace App\Support;

use Illuminate\Contracts\Pagination\LengthAwarePaginator;
use Illuminate\Http\JsonResponse;
use Symfony\Component\HttpFoundation\Response;

/**
 * Builds the success/error envelope described in PRD §10.1. `meta` always
 * carries `request_id` (set by App\Http\Middleware\RequestId) and a
 * timestamp, which is what makes production debugging traceable.
 */
final class ApiResponse
{
    /**
     * @param  array<string, mixed>  $meta
     */
    public static function success(
        mixed $data = null,
        string $message = 'OK',
        array $meta = [],
        int $status = Response::HTTP_OK,
    ): JsonResponse {
        return response()->json([
            'success' => true,
            'message' => $message,
            'data' => $data,
            'meta' => array_merge(self::baseMeta(), $meta),
        ], $status);
    }

    /**
     * @param  array<string, list<string>>  $errors
     * @param  array<string, mixed>  $meta
     */
    public static function error(
        string $message,
        string $code,
        int $status,
        array $errors = [],
        array $meta = [],
    ): JsonResponse {
        $payload = [
            'success' => false,
            'message' => $message,
            'code' => $code,
            'meta' => array_merge(self::baseMeta(), $meta),
        ];

        if ($errors !== []) {
            $payload['errors'] = $errors;
        }

        return response()->json($payload, $status);
    }

    /**
     * @param  LengthAwarePaginator<int, mixed>  $paginator
     * @return array{pagination: array{current_page: int, per_page: int, total: int, last_page: int, from: int|null, to: int|null}}
     */
    public static function paginationMeta(LengthAwarePaginator $paginator): array
    {
        return [
            'pagination' => [
                'current_page' => $paginator->currentPage(),
                'per_page' => $paginator->perPage(),
                'total' => $paginator->total(),
                'last_page' => $paginator->lastPage(),
                'from' => $paginator->firstItem(),
                'to' => $paginator->lastItem(),
            ],
        ];
    }

    /**
     * @return array{request_id: string|null, timestamp: string}
     */
    private static function baseMeta(): array
    {
        return [
            'request_id' => request()->attributes->get('request_id'),
            'timestamp' => now()->toIso8601String(),
        ];
    }
}
