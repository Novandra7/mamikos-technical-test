<?php

use App\Exceptions\Domain\DomainException;
use App\Http\Middleware\EnsureUserHasRole;
use App\Http\Middleware\RequestId;
use App\Http\Middleware\SecurityHeaders;
use App\Support\ApiResponse;
use Illuminate\Auth\Access\AuthorizationException;
use Illuminate\Auth\AuthenticationException;
use Illuminate\Database\Eloquent\ModelNotFoundException;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;
use Symfony\Component\HttpKernel\Exception\HttpExceptionInterface;
use Symfony\Component\HttpKernel\Exception\MethodNotAllowedHttpException;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;
use Symfony\Component\HttpKernel\Exception\TooManyRequestsHttpException;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
    )
    ->withMiddleware(function (Middleware $middleware): void {
        $middleware->api(prepend: [
            RequestId::class,
            SecurityHeaders::class,
        ]);

        // SEC-08: 60 requests/minute baseline for every API consumer.
        // Endpoint-specific limits (`auth`, `inquiry`) are applied per route.
        $middleware->throttleApi('api');

        $middleware->alias([
            'role' => EnsureUserHasRole::class,
        ]);
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        $exceptions->shouldRenderJsonWhen(
            fn (Request $request) => $request->is('api/*') || $request->expectsJson(),
        );

        $exceptions->render(function (DomainException $e, Request $request) {
            return ApiResponse::error(
                message: $e->getMessage(),
                code: $e->errorCode(),
                status: $e->httpStatus(),
                errors: $e->errors(),
            );
        });

        $exceptions->render(function (ValidationException $e, Request $request) {
            if (! ($request->is('api/*') || $request->expectsJson())) {
                return null;
            }

            return ApiResponse::error(
                message: 'The given data was invalid.',
                code: 'VALIDATION_ERROR',
                status: 422,
                errors: $e->errors(),
            );
        });

        $exceptions->render(function (AuthenticationException $e, Request $request) {
            if (! ($request->is('api/*') || $request->expectsJson())) {
                return null;
            }

            return ApiResponse::error(
                message: 'Authentication is required to access this resource.',
                code: 'UNAUTHENTICATED',
                status: 401,
            );
        });

        $exceptions->render(function (AuthorizationException $e, Request $request) {
            if (! ($request->is('api/*') || $request->expectsJson())) {
                return null;
            }

            return ApiResponse::error(
                message: $e->getMessage() ?: 'You do not have permission to access this resource.',
                code: 'FORBIDDEN',
                status: 403,
            );
        });

        $exceptions->render(function (ModelNotFoundException $e, Request $request) {
            if (! ($request->is('api/*') || $request->expectsJson())) {
                return null;
            }

            return ApiResponse::error(
                message: 'The requested resource could not be found.',
                code: 'RESOURCE_NOT_FOUND',
                status: 404,
            );
        });

        $exceptions->render(function (NotFoundHttpException $e, Request $request) {
            if (! ($request->is('api/*') || $request->expectsJson())) {
                return null;
            }

            return ApiResponse::error(
                message: 'The requested resource could not be found.',
                code: 'RESOURCE_NOT_FOUND',
                status: 404,
            );
        });

        $exceptions->render(function (MethodNotAllowedHttpException $e, Request $request) {
            if (! ($request->is('api/*') || $request->expectsJson())) {
                return null;
            }

            return ApiResponse::error(
                message: 'This HTTP method is not supported for this endpoint.',
                code: 'METHOD_NOT_ALLOWED',
                status: 405,
            );
        });

        $exceptions->render(function (TooManyRequestsHttpException $e, Request $request) {
            if (! ($request->is('api/*') || $request->expectsJson())) {
                return null;
            }

            $meta = $e->getHeaders()['Retry-After'] ?? null
                ? ['retry_after' => (int) $e->getHeaders()['Retry-After']]
                : [];

            return ApiResponse::error(
                message: 'Too many requests. Please try again later.',
                code: 'TOO_MANY_REQUESTS',
                status: 429,
                meta: $meta,
            );
        });

        // Catch-all: never leak a stack trace, class name, or SQL in
        // production (§10.2 mandatory rule). Anything not handled above
        // (and not an HttpExceptionInterface Laravel already renders
        // sensibly) becomes a generic 500 once APP_DEBUG is false.
        $exceptions->render(function (Throwable $e, Request $request) {
            if (! ($request->is('api/*') || $request->expectsJson())) {
                return null;
            }

            if (config('app.debug') || $e instanceof HttpExceptionInterface) {
                return null;
            }

            return ApiResponse::error(
                message: 'An unexpected error occurred.',
                code: 'INTERNAL_SERVER_ERROR',
                status: 500,
            );
        });
    })->create();
