/*
 * Copyright (c) 2017-2018 apifocal LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apifocal.amix.tools.token;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apifocal.amix.jaas.token.Settings;
import org.apifocal.amix.jaas.token.TokenValidationException;
import org.apifocal.amix.jaas.token.Tokens;
import org.apifocal.amix.jaas.token.verifiers.TokenSignerValidator;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * JMS Token Generation Utility for Apache ActiveMQ
 */
public class TokenApp {

    private static final String ACTION_HELP = "help";
    private static final String ACTION_CREATE = "create";
    private static final String ACTION_SHOW = "show";
    private static final String HELP_USAGE = "\n"
        + "  amix-token create [options] <signing-key>\n"
        + "  amix-token verify [options] [<token>]";
    private static final String CLAIM_ACL = "acl";
    private static final String DEFAULT_ACL = "rw";

    private static final Option OPTION_HELP = Option
        .builder("h").longOpt("help").build();
    private static final Option OPTION_USER = Option
        .builder("u").longOpt("user").desc("Username for creating JMS connection.").hasArg(true).argName("user").build();
    private static final Option OPTION_ISSUER = Option
        .builder("i").longOpt("issuer").desc("Issuer of authorization token (key owner)").hasArg(true).argName("issuer").build();
    private static final Option OPTION_APP = Option
        .builder("a").longOpt("app").desc("JMS application id (optional)").hasArg(true).argName("app").build();
    private static final Option OPTION_ACL = Option
        .builder().longOpt("acl").desc("Token access privileges").hasArg(true).argName("acl").build();
    private static final Option OPTION_EXPIRATION = Option
        .builder().longOpt("exp").desc("Token expiration time").hasArg(true).argName("exp").build();
    private static final Option OPTION_VERIFY = Option
        .builder().longOpt("verify").desc("Token signature verificaion").hasArg(false).build();
    private static final Option OPTION_KEYS = Option
        .builder().longOpt("keys").desc("Authorized keys file").hasArg(true).argName("keys").build();

    private static final Options OPTIONS = new Options();
    private static final Option[] OPTIONS_LIST = {
            OPTION_HELP,
            OPTION_USER,
            OPTION_ISSUER,
            OPTION_APP,
            OPTION_ACL,
            OPTION_EXPIRATION,
            OPTION_VERIFY,
            OPTION_KEYS,
    };
    private static final String OPTIONS_FOOTER = "\n"
        + "Generates JWT token as password for ActiveMQ brokers\n"
        + "For more information visit https://docs.silkmq.com";

    private static final Map<String, String> CLAIM_NAMES = new HashMap<>();
    private static final String[][] CLAIM_NAME_LIST = {
        {"sub", "User"},
        {"iss", "Issuer"},
        {"aud", "App"},
        {"iat", "Issued-At"},
        {"exp", "Expires"},
        {"nbf", "Not-Before"},
    };

    static {
        Arrays.asList(OPTIONS_LIST).forEach(o -> {
            OPTIONS.addOption(o);
        });
        Arrays.asList(CLAIM_NAME_LIST).forEach(a -> {
            CLAIM_NAMES.put(a[0], a[1]);
        });
    };

    public static void main(String[] args) {
        String action = "";
        CommandLine cli = null;

        try {
            if (args.length > 0) {
                action = args[0].toLowerCase();
                action = action.startsWith("-") ? action.replaceAll("-", "") : action;
                action = action.equals(OPTION_HELP.getOpt()) ? ACTION_HELP : action;
                cli = parse(Arrays.copyOfRange(args, 1, args.length));
            }

            boolean validArgs = !ImmutableSet.of(ACTION_HELP, ACTION_CREATE, ACTION_SHOW).contains(action) ? false
                : ACTION_CREATE.equals(action) ? validateCreateArgs(cli)
                : ACTION_SHOW.equals(action) ? validateShowArgs(cli)
                : cli.hasOption(OPTION_HELP.getOpt());
            if (!validArgs) {
                throw new ParseException("Missing required arguments");
            }
        } catch (ParseException e) {
            usage(e.getLocalizedMessage());
            return;
        }

        if (cli.hasOption("h")) {
            help(System.out);
            return;
        }

        try {
            if (ACTION_CREATE.equals(action)) {
                String token = createToken(cli);
                System.out.println(token != null ? token : "ERROR");
            } else if (ACTION_SHOW.equals(action)) {
                showToken(cli);
            }
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    private static boolean validateCreateArgs(CommandLine cli) {
        // TODO: maybe log some verbose warnings
        return cli.hasOption("u") && cli.hasOption("i") && cli.getArgs().length == 1;
    }

    private static boolean validateShowArgs(CommandLine cli) {
        return cli.getArgs().length == 1 && (!cli.hasOption("verify") || cli.hasOption("keys"));
    }

    public static String createToken(final CommandLine cli) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder();
        Tokens.subject(claims, Objects.requireNonNull(cli.getOptionValue("u"), "Missing option 'user'"));
        Tokens.issuer(claims, Objects.requireNonNull(cli.getOptionValue("i"), "Missing option 'issuer'"));

        String[] audience = cli.getOptionValues("a");
        if (audience != null) {
            if (audience.length == 1) {
                Tokens.audience(claims, audience[0]);
            } else if (audience.length > 1) {
                Tokens.audience(claims, Arrays.asList(audience));
            }            
        }
        String acl = cli.getOptionValue("acl");
        Tokens.claim(claims, CLAIM_ACL, acl != null ? acl : DEFAULT_ACL);

        String key =  cli.getArgs()[0]; // if argument validation was done well, this could not be null
        Path sk = Paths.get(key);
        if (!Files.exists(sk) || !Files.isRegularFile(sk)) {
            throw new IllegalArgumentException("Invalid signing key. Check command line arguments.");
        }

        // TODO: make it optional?
        Date now = new Date();
        Tokens.issueTime(claims, now);
        Tokens.expiration(claims, new Date(now.getTime() + lifespan(cli.getOptionValue("exp"))));

        return Tokens.createToken(claims.build(), new String(Files.readAllBytes(sk), Charsets.UTF_8), new StdinPasswordProvider(key));
    }

    public static void showToken(final CommandLine cli) {
        SignedJWT parsedToken;
        Map<String, Object> claims;
        try {
            parsedToken = SignedJWT.parse(cli.getArgs()[0]);
            claims = new HashMap<>(parsedToken.getJWTClaimsSet().getClaims());
        } catch (java.text.ParseException e) {
            System.out.println("Not a valid signed token.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        if (cli.hasOption("verify")) {
            builder.append("Signature verification: ");

            Map<String, String> settings = new HashMap<>();
            settings.put("keys", Objects.requireNonNull(cli.getOptionValue("keys")));
            TokenSignerValidator validator = new TokenSignerValidator(new Settings(settings));
            try {
                validator.validate(parsedToken, null);
                builder.append("PASSED.");
            } catch (TokenValidationException e) {
                builder.append("FAILED:\n").append("  ").append(e.getLocalizedMessage());
            }
            builder.append('\n').append('\n');
        }
        builder.append("Claims:\n");
        printClaims(claims, builder);
        System.out.println(builder.toString());
    }

    private static void printClaims(Map<String, Object> claims, final StringBuilder builder) {
        printKnownClaim(claims, builder, "sub");
        printKnownClaim(claims, builder, "iss");
        printKnownClaim(claims, builder, "aud");
        printKnownClaim(claims, builder, "iat");
        printKnownClaim(claims, builder, "exp");
        printKnownClaim(claims, builder, "nbf");
        claims.forEach((k, v) -> {
            printClaim(builder, k, v.toString());
        });
    }

    private static void printKnownClaim(Map<String, Object> claims, final StringBuilder builder, String name) {
        String claimName = CLAIM_NAMES.get(name);
        Object value = claims.remove(name);
        if (value != null) {
            printClaim(builder, claimName == null ? name : claimName, value);
        }
    }

    private static void printClaim(final StringBuilder builder, String name, Object value) {
        String claimName = CLAIM_NAMES.get(name);
        claimName = claimName == null ? name : claimName;

        // value cannot be null in this context
        String text = value.toString();
        if (value instanceof Date) {
            text = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date)value);
        }
        builder.append("  ").append(claimName).append(": ").append(text).append("\n");
    }

    private static long lifespan(String exp) {
        long delta = Tokens.days(90); // default
        try {
            if (exp != null) {
                char last = exp.charAt(exp.length() - 1);
                if (Character.isLetter(last)) {
                    int num = Integer.parseInt(exp.substring(0, exp.length() - 1));
                    switch (last) {
                    case 'Z': break; // TODO: add support for explicitly defined time
                    case 'd': delta = Tokens.days(num); break;
                    case 'h': delta = Tokens.hours(num); break;
                    case 'm': delta = Tokens.minutes(num); break;
                    default: break; // TODO: report unsupported qualifier; use default
                    }
                } else {
                    return Tokens.seconds(Integer.parseInt(exp));
                }
            }
        } catch (Exception e) {
            // TODO: report
        }
        return delta;
    }

    public static CommandLine parse(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        return parser.parse(OPTIONS, args);
    }

    public static void usage() {
        usage(null);
    }

    public static void usage(String message) {
        if (message != null && !message.isEmpty()) {
            System.out.println(message);
        }
        help(System.out);
    }

    private static void help(PrintStream out) {
        out.println();

        PrintWriter pw = new PrintWriter(out);
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(new CustomComparator());
        formatter.printHelp(pw, HelpFormatter.DEFAULT_WIDTH, HELP_USAGE, "\nOptions:", filterOptions(OPTIONS),
             HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, OPTIONS_FOOTER, false);
        pw.flush();
    }

    private static Options filterOptions(Options options) {
        Options result = new Options();
        options.getOptions().forEach(o -> {
            String desc = o.getDescription();
            if (desc == null || !o.getDescription().startsWith("[NO HELP]")) {
                result.addOption(o);
            }
        });
        return result;
    }

    private static class CustomComparator implements Comparator<Option>, Serializable {
        private static final long serialVersionUID = 1L;

        public int compare(Option o1, Option o2) {
            return index(o1.getId()) - index(o2.getId());
        }

        private int index(int opt) {
            // not very pretty, but fast;
            // ...not that it matters much if we reached this point
            int result = 0;
            switch (opt) {
                case 'h': result = 1; break;
                case 'u': result = 2; break;
                case 'i': result = 3; break;
                case 'a': result = 4; break;
                default: result = 16;
            }
            return result;
        }
    }
}
