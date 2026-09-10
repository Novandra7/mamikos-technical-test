<?php

declare(strict_types=1);

use App\Http\Controllers\Api\V1\AuthController;
use App\Http\Controllers\Api\V1\AvailabilityInquiryController;
use App\Http\Controllers\Api\V1\CreditController;
use App\Http\Controllers\Api\V1\HealthController;
use App\Http\Controllers\Api\V1\KostSearchController;
use App\Http\Controllers\Api\V1\Owner\OwnerInquiryController;
use App\Http\Controllers\Api\V1\Owner\OwnerKostController;
use Illuminate\Support\Facades\Route;

Route::prefix('v1')->group(function (): void {
    // FR-20 / REL-01
    Route::get('/health', HealthController::class);

    Route::prefix('auth')->group(function (): void {
        Route::post('/register', [AuthController::class, 'register'])->middleware('throttle:auth');
        Route::post('/login', [AuthController::class, 'login'])->middleware('throttle:auth');

        Route::middleware('auth:sanctum')->group(function (): void {
            Route::post('/logout', [AuthController::class, 'logout']);
            Route::post('/refresh', [AuthController::class, 'refresh']);
            Route::get('/me', [AuthController::class, 'me']);
        });
    });

    // US-06 / US-07: public search & detail, no authentication required.
    Route::get('/kosts', [KostSearchController::class, 'index']);
    Route::get('/kosts/{kost}', [KostSearchController::class, 'show']);

    Route::middleware('auth:sanctum')->group(function (): void {
        // US-08: only REGULAR/PREMIUM own a wallet to spend.
        Route::middleware(['role:regular,premium', 'throttle:inquiry'])->group(function (): void {
            Route::post('/kosts/{kost}/availability-inquiries', [AvailabilityInquiryController::class, 'store']);
        });

        Route::middleware('role:regular,premium')->group(function (): void {
            Route::get('/me/inquiries', [AvailabilityInquiryController::class, 'myInquiries']);
            Route::get('/me/credits', [CreditController::class, 'balance']);
            Route::get('/me/credits/transactions', [CreditController::class, 'transactions']);
        });

        // US-03..US-05, US-10: owner-only kost management & inquiry inbox.
        Route::prefix('owner')->middleware('role:owner')->group(function (): void {
            Route::post('/kosts', [OwnerKostController::class, 'store']);
            Route::get('/kosts', [OwnerKostController::class, 'index']);
            Route::get('/kosts/{kost}', [OwnerKostController::class, 'show']);
            Route::put('/kosts/{kost}', [OwnerKostController::class, 'update']);
            Route::patch('/kosts/{kost}', [OwnerKostController::class, 'update']);
            Route::delete('/kosts/{kost}', [OwnerKostController::class, 'destroy']);

            Route::get('/inquiries', [OwnerInquiryController::class, 'index']);
            Route::post('/inquiries/{inquiry}/reply', [OwnerInquiryController::class, 'reply']);
        });
    });
});
