<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('credit_balances', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained('users')->cascadeOnDelete()->unique();
            $table->integer('balance')->default(0);
            $table->unsignedBigInteger('version')->default(0);
            $table->timestamp('last_recharged_at')->nullable();
            $table->timestamps();
        });

        // BR-03: the balance must never go negative, enforced at the database
        // level in addition to the pessimistic lock in the application layer.
        if (DB::getDriverName() === 'mysql') {
            DB::statement('ALTER TABLE credit_balances ADD CONSTRAINT chk_credit_balances_non_negative CHECK (balance >= 0)');
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('credit_balances');
    }
};
