<?php

declare(strict_types=1);

namespace Tests\Feature\Auth;

use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Laravel\Sanctum\Sanctum;
use Tests\TestCase;

class LogoutTest extends TestCase
{
    use RefreshDatabase;

    public function test_logout_revokes_the_current_token(): void
    {
        $user = User::factory()->regular()->create();
        $token = $user->createToken('api');

        $response = $this->withHeader('Authorization', 'Bearer '.$token->plainTextToken)
            ->postJson('/api/v1/auth/logout');

        $response->assertOk();
        $this->assertDatabaseMissing('personal_access_tokens', ['id' => $token->accessToken->id]);
    }

    public function test_refresh_revokes_the_old_token_and_issues_a_new_one(): void
    {
        $user = User::factory()->regular()->create();
        $token = $user->createToken('api');

        $response = $this->withHeader('Authorization', 'Bearer '.$token->plainTextToken)
            ->postJson('/api/v1/auth/refresh');

        $response->assertOk();
        $newToken = $response->json('data.token.access_token');

        $this->assertNotSame($token->plainTextToken, $newToken);
        $this->assertDatabaseMissing('personal_access_tokens', ['id' => $token->accessToken->id]);
    }

    public function test_me_requires_authentication(): void
    {
        $this->getJson('/api/v1/auth/me')->assertStatus(401)->assertJsonPath('code', 'UNAUTHENTICATED');
    }

    public function test_me_returns_profile_and_balance(): void
    {
        Sanctum::actingAs($user = User::factory()->regular()->create(), ['*']);
        $user->creditBalance()->create(['balance' => 20]);

        $response = $this->getJson('/api/v1/auth/me');

        $response->assertOk()->assertJsonPath('data.user.email', $user->email);
    }
}
