<?php

declare(strict_types=1);

namespace App\Http\Middleware;

use App\Support\ApiResponse;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * SEC-03: route-level role gate, e.g. `role:owner` or `role:regular,premium`.
 * Kept separate from Sanctum's `auth:sanctum` middleware so "not
 * authenticated" (401) and "wrong role" (403) stay distinct error codes.
 */
class EnsureUserHasRole
{
    public function handle(Request $request, Closure $next, string ...$roles): Response
    {
        $user = $request->user();

        if ($user === null || ! in_array($user->role->value, $roles, true)) {
            return ApiResponse::error(
                message: 'You do not have permission to access this resource.',
                code: 'FORBIDDEN',
                status: 403,
            );
        }

        return $next($request);
    }
}
