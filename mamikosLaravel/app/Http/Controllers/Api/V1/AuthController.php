<?php

declare(strict_types=1);

namespace App\Http\Controllers\Api\V1;

use App\DTOs\Auth\RegisterUserData;
use App\Http\Controllers\Controller;
use App\Http\Requests\Auth\LoginRequest;
use App\Http\Requests\Auth\RegisterRequest;
use App\Http\Resources\UserResource;
use App\Models\User;
use App\Services\AuthService;
use App\Services\CreditService;
use App\Support\ApiResponse;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Laravel\Sanctum\NewAccessToken;

class AuthController extends Controller
{
    public function __construct(
        private readonly AuthService $authService,
        private readonly CreditService $creditService,
    ) {}

    public function register(RegisterRequest $request): JsonResponse
    {
        $data = RegisterUserData::fromArray($request->validated());

        $result = $this->authService->register($data);

        return ApiResponse::success(
            data: [
                'user' => new UserResource($result['user']),
                'credit' => $this->creditSummary($result['user']),
                'token' => $this->tokenPayload($result['token']),
            ],
            message: 'Registration successful',
            status: 201,
        );
    }

    public function login(LoginRequest $request): JsonResponse
    {
        $result = $this->authService->login($request->string('email')->toString(), $request->string('password')->toString());

        return ApiResponse::success(
            data: [
                'user' => new UserResource($result['user']),
                'credit' => $this->creditSummary($result['user']),
                'token' => $this->tokenPayload($result['token']),
            ],
            message: 'Login successful',
        );
    }

    public function logout(Request $request): JsonResponse
    {
        $this->authService->logout($request->user());

        return ApiResponse::success(message: 'Logged out successfully');
    }

    public function refresh(Request $request): JsonResponse
    {
        $result = $this->authService->refresh($request->user());

        return ApiResponse::success(
            data: [
                'user' => new UserResource($result['user']),
                'credit' => $this->creditSummary($result['user']),
                'token' => $this->tokenPayload($result['token']),
            ],
            message: 'Token refreshed',
        );
    }

    public function me(Request $request): JsonResponse
    {
        $user = $this->authService->me($request->user());

        return ApiResponse::success(
            data: [
                'user' => new UserResource($user),
                'credit' => $this->creditSummary($user),
            ],
            message: 'Profile retrieved',
        );
    }

    /**
     * @return array{balance: int, quota: int}|null
     */
    private function creditSummary(User $user): ?array
    {
        if (! $user->role->hasCreditWallet()) {
            return null;
        }

        $balance = $this->creditService->balanceFor($user->id);
        $currentBalance = $balance === null ? 0 : $balance->balance;

        return [
            'balance' => $currentBalance,
            'quota' => $this->creditService->quotaFor($user->role),
        ];
    }

    /**
     * @return array{access_token: string, token_type: string, expires_in: int}
     */
    private function tokenPayload(NewAccessToken $token): array
    {
        return [
            'access_token' => $token->plainTextToken,
            'token_type' => 'Bearer',
            'expires_in' => $this->authService->tokenExpiresInSeconds(),
        ];
    }
}
