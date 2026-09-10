<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('availability_inquiries', function (Blueprint $table) {
            $table->id();
            $table->foreignId('kost_id')->constrained('kosts')->cascadeOnDelete();
            $table->foreignId('user_id')->constrained('users')->cascadeOnDelete();
            $table->text('message')->nullable();
            $table->integer('credit_charged');
            $table->integer('available_rooms_snapshot');
            $table->string('status', 20)->default('PENDING');
            $table->text('owner_reply')->nullable();
            $table->timestamp('replied_at')->nullable();
            $table->timestamps();

            $table->index(['user_id', 'created_at']);
            $table->index(['kost_id', 'status']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('availability_inquiries');
    }
};
