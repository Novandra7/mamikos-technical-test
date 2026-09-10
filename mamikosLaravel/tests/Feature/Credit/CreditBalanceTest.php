<?php

declare(strict_types=1);

namespace Tests\Feature\Credit;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\Concerns\CreatesUsers;
use Tests\TestCase;

class CreditBalanceTest extends TestCase
{
    use CreatesUsers, RefreshDatabase;

    public function test_regular_user_can_see_their_balance(): void
    {
        Sanctum::actingAs($this->createRegular(balance: 15), ['*']);

        $response = $this->getJson('/api/v1/me/credits');

        $response->assertOk()
            ->assertJsonPath('data.balance', 15)
            ->assertJsonPath('data.quota', 20)
            ->assertJsonPath('data.inquiry_cost', 5)
            ->assertJsonPath('data.remaining_inquiries', 3);
    }

    public function test_owner_cannot_access_credit_endpoints(): void
    {
        Sanctum::actingAs(User::factory()->owner()->create(), ['*']);

        $this->getJson('/api/v1/me/credits')->assertStatus(403)->assertJsonPath('code', 'FORBIDDEN');
    }

    public function test_unauthenticated_access_is_rejected(): void
    {
        $this->getJson('/api/v1/me/credits')->assertStatus(401);
    }
}
