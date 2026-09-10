<?php

declare(strict_types=1);

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Symfony\Component\HttpFoundation\Response;

/**
 * Stamps every request with a UUIDv7 request id (PRD §10.1). It is stored
 * on the request so ApiResponse can put it in `meta.request_id`, added as
 * the `X-Request-Id` response header, and merged into the log context so
 * every log line for this request can be correlated.
 */
class RequestId
{
    public function handle(Request $request, Closure $next): Response
    {
        $requestId = $request->header('X-Request-Id') ?: (string) Str::uuid7();

        $request->attributes->set('request_id', $requestId);

        logger()->withContext(['request_id' => $requestId]);

        /** @var Response $response */
        $response = $next($request);

        $response->headers->set('X-Request-Id', $requestId);

        return $response;
    }
}
