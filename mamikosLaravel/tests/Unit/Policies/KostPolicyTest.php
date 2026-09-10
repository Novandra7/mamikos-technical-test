<?php

declare(strict_types=1);

namespace Tests\Unit\Policies;

use App\Models\Kost;
use App\Models\User;
use App\Policies\KostPolicy;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class KostPolicyTest extends TestCase
{
    use RefreshDatabase;

    private KostPolicy $policy;

    protected function setUp(): void
    {
        parent::setUp();

        $this->policy = new KostPolicy;
    }

    public function test_owner_can_update_own_kost(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create();

        $this->assertTrue($this->policy->update($owner, $kost));
        $this->assertTrue($this->policy->delete($owner, $kost));
    }

    public function test_owner_cannot_update_another_owners_kost(): void
    {
        $ownerA = User::factory()->owner()->create();
        $ownerB = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($ownerB)->create();

        $this->assertFalse($this->policy->update($ownerA, $kost));
        $this->assertFalse($this->policy->delete($ownerA, $kost));
    }
}
