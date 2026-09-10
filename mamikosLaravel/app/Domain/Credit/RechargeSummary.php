<?php

declare(strict_types=1);

namespace App\Domain\Credit;

/**
 * Result of one `credits:recharge` run, reported to the console table and
 * to the structured log (US-11).
 */
final class RechargeSummary
{
    public int $processed = 0;

    public int $recharged = 0;

    public int $skipped = 0;

    public int $failed = 0;

    public float $durationSeconds = 0.0;

    public function incrementProcessed(): void
    {
        $this->processed++;
    }

    public function incrementRecharged(): void
    {
        $this->recharged++;
    }

    public function incrementSkipped(): void
    {
        $this->skipped++;
    }

    public function incrementFailed(): void
    {
        $this->failed++;
    }

    /**
     * @return array{0: int, 1: int, 2: int, 3: int, 4: string}
     */
    public function toRow(): array
    {
        return [
            $this->processed,
            $this->recharged,
            $this->skipped,
            $this->failed,
            number_format($this->durationSeconds, 2),
        ];
    }

    /**
     * @return array{processed: int, recharged: int, skipped: int, failed: int, duration_seconds: float}
     */
    public function toArray(): array
    {
        return [
            'processed' => $this->processed,
            'recharged' => $this->recharged,
            'skipped' => $this->skipped,
            'failed' => $this->failed,
            'duration_seconds' => round($this->durationSeconds, 2),
        ];
    }
}
