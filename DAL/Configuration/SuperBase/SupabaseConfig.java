package com.group130.laundryapp.laundry2_0.DAL.Configuration.SuperBase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * SupabaseConfig
 *
 * Builds a WebClient for writing to Supabase's PostgREST API using
 * the SERVICE ROLE key — full read/write access, used ONLY from
 * the backend, never exposed to the frontend.
 *
 * The frontend uses a separate, public ANON key (safe to embed in
 * the app) to subscribe to Realtime changes — that key only allows
 * reads, scoped further by a Row Level Security policy you set up
 * once in the Supabase dashboard SQL editor:
 *
 *   ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
 *
 *   CREATE POLICY "Allow read for anon"
 *   ON messages FOR SELECT
 *   TO anon
 *   USING (true);
 *   -- Reads are filtered client-side by conversation_id in the
 *   -- subscription query — see frontend notes in MessageController.
 *   -- Inserts/updates/deletes are NOT allowed for anon — only
 *   -- your backend's service-role key can write.
 */
@Configuration
public class SupabaseConfig {

    @Value("${supabase.project-url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Bean
    public WebClient supabaseWebClient() {
        return WebClient.builder()
                .baseUrl(supabaseUrl + "/rest/v1")
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Prefer", "return=representation")
                .build();
    }
}
