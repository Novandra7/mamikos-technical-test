<?php

declare(strict_types=1);

namespace Tests\Feature\Auth;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class LoginTest extends TestCase
{
    use RefreshDatabase;

    public function test_correct_credentials_return_a_token(): void
    {
        User::factory()->regular()->create([
            'email' => 'user@example.com',
            'password' => Hash::make('Password123'),
        ]);

        $response = $this->postJson('/api/v1/auth/login', [
            'email' => 'user@example.com',
            'password' => 'Password123',
        ]);

        $response->assertOk()
            ->assertJsonPath('data.token.token_type', 'Bearer')
            ->assertJsonPath('data.token.expires_in', 86400);
    }

    public function test_wrong_password_returns_generic_invalid_credentials_error(): void
    {
        User::factory()->regular()->create([
            'email' => 'user@example.com',
            'password' => Hash::make('Password123'),
        ]);

        $response = $this->postJson('/api/v1/auth/login', [
            'email' => 'user@example.com',
            'password' => 'WrongPassword1',
        ]);

        $response->assertStatus(401)->assertJsonPath('code', 'INVALID_CREDENTIALS');
    }

    public function test_unknown_email_returns_the_same_generic_error(): void
    {
        $response = $this->postJson('/api/v1/auth/login', [
            'email' => 'ghost@example.com',
            'password' => 'WhoKnows123',
        ]);

        $response->assertStatus(401)->assertJsonPath('code', 'INVALID_CREDENTIALS');
    }

    public function test_login_is_rate_limited_after_five_attempts(): void
    {
        for ($i = 0; $i < 5; $i++) {
            $this->postJson('/api/v1/auth/login', ['email' => 'x@example.com', 'password' => 'wrong']);
        }

        $response = $this->postJson('/api/v1/auth/login', ['email' => 'x@example.com', 'password' => 'wrong']);

        $response->assertStatus(429)->assertJsonPath('code', 'TOO_MANY_REQUESTS');
    }
}
