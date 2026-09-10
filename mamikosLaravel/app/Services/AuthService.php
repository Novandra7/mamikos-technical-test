<?php

declare(strict_types=1);

namespace App\Services;

use App\DTOs\Auth\RegisterUserData;
use App\Exceptions\Domain\EmailAlreadyRegisteredException;
use App\Exceptions\Domain\InvalidCredentialsException;
use App\Models\User;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Laravel\Sanctum\NewAccessToken;

final class AuthService
{
    public function __construct(private readonly CreditService $creditService) {}

    /**
     * @return array{user: User, token: NewAccessToken}
     *
     * @throws EmailAlreadyRegisteredException
     */
    public function register(RegisterUserData $data): array
    {
        if (User::withTrashed()->where('email', $data->email)->exists()) {
            throw new EmailAlreadyRegisteredException;
        }

        // BR-10: user + wallet + initial ledger row must succeed or fail
        // together.
        $user = DB::transaction(function () use ($data): User {
            /** @var User $user */
            $user = User::create([
                'name' => $data->name,
                'email' => $data->email,
                'password' => Hash::make($data->password),
                'phone' => $data->phone,
                'role' => $data->role,
            ]);

            $this->creditService->grantInitial($user->id, $data->role);

            return $user;
        });

        $token = $user->createToken('api');

        return ['user' => $user->fresh('creditBalance'), 'token' => $token];
    }

    /**
     * @return array{user: User, token: NewAccessToken}
     *
     * @throws InvalidCredentialsException
     */
    public function login(string $email, string $password): array
    {
        $user = User::where('email', mb_strtolower($email))->first();

        if (! $user || ! Hash::check($password, $user->password)) {
            throw new InvalidCredentialsException;
        }

        $token = $user->createToken('api');

        return ['user' => $user->loadMissing('creditBalance'), 'token' => $token];
    }

    public function logout(User $user): void
    {
        $user->currentAccessToken()->delete();
    }

    /**
     * Rotates the currently used token: the old one is revoked and a new
     * one is issued (BR-15).
     *
     * @return array{user: User, token: NewAccessToken}
     */
    public function refresh(User $user): array
    {
        $user->currentAccessToken()->delete();

        $token = $user->createToken('api');

        return ['user' => $user->loadMissing('creditBalance'), 'token' => $token];
    }

    public function me(User $user): User
    {
        return $user->loadMissing('creditBalance');
    }

    public function tokenExpiresInSeconds(): int
    {
        $minutes = (int) config('sanctum.expiration', 1440);

        return $minutes * 60;
    }
}
