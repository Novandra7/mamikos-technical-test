<?php

declare(strict_types=1);

namespace Tests\Unit\Policies;

use App\Models\AvailabilityInquiry;
use App\Models\Kost;
use App\Models\User;
use App\Policies\InquiryPolicy;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class InquiryPolicyTest extends TestCase
{
    use RefreshDatabase;

    private InquiryPolicy $policy;

    protected function setUp(): void
    {
        parent::setUp();

        $this->policy = new InquiryPolicy;
    }

    public function test_kost_owner_can_view_and_reply_to_its_inquiries(): void
    {
        $owner = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create();
        $inquiry = AvailabilityInquiry::factory()->for($kost)->create();

        $this->assertTrue($this->policy->view($owner, $inquiry));
        $this->assertTrue($this->policy->reply($owner, $inquiry));
    }

    public function test_a_different_owner_cannot_view_or_reply(): void
    {
        $owner = User::factory()->owner()->create();
        $intruder = User::factory()->owner()->create();
        $kost = Kost::factory()->forOwner($owner)->create();
        $inquiry = AvailabilityInquiry::factory()->for($kost)->create();

        $this->assertFalse($this->policy->view($intruder, $inquiry));
        $this->assertFalse($this->policy->reply($intruder, $inquiry));
    }
}
