<?php

declare(strict_types=1);

namespace App\DTOs\Auth;

use App\Enums\UserRole;

final readonly class RegisterUserData
{
    public function __construct(
        public string $name,
        public string $email,
        public string $password,
        public ?string $phone,
        public UserRole $role,
    ) {}

    /**
     * @param  array{name: string, email: string, password: string, phone?: string|null, role: string}  $attributes
     */
    public static function fromArray(array $attributes): self
    {
        return new self(
            name: $attributes['name'],
            email: mb_strtolower($attributes['email']),
            password: $attributes['password'],
            phone: $attributes['phone'] ?? null,
            role: UserRole::from($attributes['role']),
        );
    }
}
