<?php

declare(strict_types=1);

namespace Tests\Feature\Auth;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class RegisterTest extends TestCase
{
    use RefreshDatabase;

    private array $payload = [
        'name' => 'Budi Santoso',
        'email' => 'budi@example.com',
        'password' => 'Password123',
        'password_confirmation' => 'Password123',
        'phone' => '081234567890',
        'role' => 'regular',
    ];

    public function test_registering_as_regular_grants_twenty_credits(): void
    {
        $response = $this->postJson('/api/v1/auth/register', $this->payload);

        $response->assertCreated()
            ->assertJsonPath('success', true)
            ->assertJsonPath('data.credit.balance', 20)
            ->assertJsonPath('data.credit.quota', 20);

        $this->assertDatabaseHas('credit_transactions', [
            'type' => 'INITIAL_GRANT',
            'amount' => 20,
        ]);
    }

    public function test_registering_as_premium_grants_forty_credits(): void
    {
        $response = $this->postJson('/api/v1/auth/register', [...$this->payload, 'email' => 'dimas@example.com', 'role' => 'premium']);

        $response->assertCreated()->assertJsonPath('data.credit.balance', 40);
    }

    public function test_registering_as_owner_creates_no_wallet(): void
    {
        $response = $this->postJson('/api/v1/auth/register', [...$this->payload, 'email' => 'owner@example.com', 'role' => 'owner']);

        $response->assertCreated()->assertJsonPath('data.credit', null);

        $this->assertDatabaseCount('credit_balances', 0);
    }

    public function test_duplicate_email_is_rejected(): void
    {
        User::factory()->regular()->create(['email' => 'budi@example.com']);

        $response = $this->postJson('/api/v1/auth/register', $this->payload);

        $response->assertStatus(409)->assertJsonPath('code', 'EMAIL_ALREADY_REGISTERED');
    }

    public function test_invalid_role_is_rejected(): void
    {
        $response = $this->postJson('/api/v1/auth/register', [...$this->payload, 'role' => 'admin']);

        $response->assertStatus(422)->assertJsonPath('code', 'VALIDATION_ERROR');
    }

    public function test_weak_password_is_rejected(): void
    {
        $response = $this->postJson('/api/v1/auth/register', [
            ...$this->payload,
            'password' => 'onlyletters',
            'password_confirmation' => 'onlyletters',
        ]);

        $response->assertStatus(422);
    }

    public function test_password_is_never_present_in_the_response(): void
    {
        $response = $this->postJson('/api/v1/auth/register', $this->payload);

        $response->assertJsonMissingPath('data.user.password');
    }
}
