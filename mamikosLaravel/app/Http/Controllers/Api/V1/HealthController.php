<?php

declare(strict_types=1);

namespace App\Http\Controllers\Api\V1;

use App\Http\Controllers\Controller;
use App\Support\ApiResponse;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Redis;
use Throwable;

/**
 * REL-01: reports whether the dependencies the API actually needs
 * (database, cache/queue broker) are reachable, so an uptime monitor can
 * alert before requests start failing.
 */
class HealthController extends Controller
{
    public function __invoke(): JsonResponse
    {
        $database = $this->check(function (): bool {
            DB::connection()->getPdo();

            return true;
        });
        $cache = $this->check(fn () => Redis::connection()->ping() !== false);

        $healthy = $database && $cache;

        return ApiResponse::success(
            data: [
                'status' => $healthy ? 'ok' : 'degraded',
                'database' => $database ? 'ok' : 'unavailable',
                'cache' => $cache ? 'ok' : 'unavailable',
            ],
            message: $healthy ? 'Service is healthy' : 'Service is degraded',
            status: $healthy ? 200 : 503,
        );
    }

    private function check(callable $probe): bool
    {
        try {
            return (bool) $probe();
        } catch (Throwable) {
            return false;
        }
    }
}
