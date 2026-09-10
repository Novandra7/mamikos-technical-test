<?php

declare(strict_types=1);

namespace Tests\Feature\Inquiry;

use App\Models\CreditTransaction;
use App\Models\Kost;
use App\Models\User;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;
use Tests\Concerns\CreatesUsers;
use Tests\TestCase;

/**
 * US-08's hardest requirement: 10 parallel requests against a single
 * 20-credit wallet must yield exactly 4 successes and leave the balance
 * at exactly 0 — never negative, never double-spent.
 *
 * This genuinely needs multiple database connections racing each other,
 * which a single, transaction-wrapped RefreshDatabase test cannot
 * reproduce (everything would run on one connection, serialized). So this
 * test forks real child processes — each reconnects to the database and
 * fires one request through the framework's HTTP test client — and
 * manages its own schema/data instead of using RefreshDatabase.
 */
class ConcurrentInquiryTest extends TestCase
{
    use CreatesUsers;

    protected function setUp(): void
    {
        parent::setUp();

        if (! function_exists('pcntl_fork')) {
            $this->markTestSkipped('pcntl extension is required to exercise real cross-connection concurrency.');
        }

        $this->truncateTables();
    }

    protected function tearDown(): void
    {
        $this->truncateTables();

        parent::tearDown();
    }

    public function test_ten_parallel_inquiries_against_twenty_credit_never_oversell(): void
    {
        $kost = Kost::factory()->create(['available_rooms' => 5, 'total_rooms' => 10]);
        $user = $this->createRegular(balance: 20);
        $userId = $user->id;
        $kostId = $kost->id;

        $concurrency = 10;
        $resultsFile = tempnam(sys_get_temp_dir(), 'inquiry_race_');
        $pids = [];

        for ($i = 0; $i < $concurrency; $i++) {
            $pid = pcntl_fork();

            if ($pid === -1) {
                $this->fail('Unable to fork a worker process for the concurrency test.');
            }

            if ($pid === 0) {
                DB::reconnect();

                $token = User::find($userId)->createToken('race-test')->plainTextToken;

                $status = $this->withHeader('Authorization', 'Bearer '.$token)
                    ->postJson("/api/v1/kosts/{$kostId}/availability-inquiries", ['message' => 'race'])
                    ->getStatusCode();

                file_put_contents($resultsFile, $status.PHP_EOL, FILE_APPEND | LOCK_EX);

                exit(0);
            }

            $pids[] = $pid;
        }

        foreach ($pids as $pid) {
            pcntl_waitpid($pid, $status);
        }

        DB::reconnect();

        $statuses = array_filter(explode(PHP_EOL, (string) file_get_contents($resultsFile)));
        @unlink($resultsFile);

        $successes = count(array_filter($statuses, fn (string $s) => $s === '201'));
        $rejections = count(array_filter($statuses, fn (string $s) => $s === '422'));

        $this->assertSame(4, $successes, 'Expected exactly 4 requests to succeed against a 20-credit balance.');
        $this->assertSame(6, $rejections, 'Expected exactly 6 requests to be rejected as INSUFFICIENT_CREDIT.');
        $this->assertSame(0, User::find($userId)->creditBalance->balance);
        $this->assertSame(
            4,
            CreditTransaction::where('user_id', $userId)->where('type', 'INQUIRY_DEDUCTION')->count(),
        );
    }

    private function truncateTables(): void
    {
        Schema::disableForeignKeyConstraints();

        foreach (['availability_inquiries', 'credit_transactions', 'credit_balances', 'personal_access_tokens', 'kost_facilities', 'kost_photos', 'kosts', 'users'] as $table) {
            DB::table($table)->truncate();
        }

        Schema::enableForeignKeyConstraints();
    }
}
