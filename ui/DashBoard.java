package ui;

import Routing.graph;
import Simulation.SimulationController;
import monitoring.DashboardSnapshot;
import monitoring.ServerSnapshot;
import monitoring.SystemEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashBoard extends JFrame {

    private static final Color PAGE_TOP = new Color(6, 16, 30);
    private static final Color PAGE_MIDDLE = new Color(10, 29, 48);
    private static final Color PAGE_BOTTOM = new Color(8, 34, 54);
    private static final Color CARD_FILL = new Color(9, 20, 35, 230);
    private static final Color CARD_BORDER = new Color(112, 176, 223, 58);
    private static final Color CARD_SHADOW = new Color(2, 9, 18, 100);
    private static final Color TEXT_PRIMARY = new Color(241, 247, 255);
    private static final Color TEXT_SECONDARY = new Color(158, 187, 212);
    private static final Color TEXT_MUTED = new Color(120, 149, 175);
    private static final Color ACCENT_CYAN = new Color(79, 214, 255);
    private static final Color ACCENT_GREEN = new Color(68, 230, 161);
    private static final Color ACCENT_ORANGE = new Color(255, 174, 86);
    private static final Color ACCENT_RED = new Color(255, 107, 129);
    private static final Color ACCENT_GOLD = new Color(255, 224, 122);
    private static final Color ACCENT_BLUE = new Color(92, 132, 255);
    private static final int HISTORY_LIMIT = 40;

    private final DecimalFormat numberFormat = new DecimalFormat("0.0");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);

    private final List<Integer> networkNodes;
    private final List<int[]> networkConnections;
    private final SimulationController simulationController;

    private final MetricCard queueCard;
    private final MetricCard requestCard;
    private final MetricCard throughputCard;
    private final MetricCard ratioCard;
    private final MetricCard latencyCard;

    private final JLabel liveStatusLabel;
    private final JLabel updatedAtLabel;
    private final JLabel insightLabel;
    private final JLabel controlStateLabel;
    private final JLabel rateLabel;
    private final JLabel controlHintLabel;
    private final JLabel serverSummaryLabel;

    private final JButton startPauseButton;
    private final JSlider rateSlider;

    private final JPanel serverListPanel;
    private final JPanel eventListPanel;
    private final TopologyPanel topologyPanel;
    private final TrendPanel trendPanel;

    private final List<Integer> queueHistory = new ArrayList<>();
    private final List<Integer> throughputHistory = new ArrayList<>();
    private final List<Double> hitRatioHistory = new ArrayList<>();

    private DashboardSnapshot latestSnapshot;
    private int lastTotalRequests = 0;

    public DashBoard(graph network, SimulationController simulationController) {
        this.networkNodes = new ArrayList<>(network.getServers());
        Collections.sort(this.networkNodes);
        this.networkConnections = network.getConnections();
        this.simulationController = simulationController;

        setTitle("Distributed Cache Load Balancer Control Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1260, 860));
        setSize(1500, 940);
        setLocationRelativeTo(null);

        queueCard = new MetricCard("QUEUE DEPTH", ACCENT_CYAN);
        requestCard = new MetricCard("REQUESTS PROCESSED", ACCENT_GOLD);
        throughputCard = new MetricCard("THROUGHPUT", ACCENT_GREEN);
        ratioCard = new MetricCard("CACHE HIT RATIO", ACCENT_ORANGE);
        latencyCard = new MetricCard("AVERAGE LATENCY", ACCENT_RED);

        liveStatusLabel = createPillLabel();
        updatedAtLabel = createBodyLabel("Waiting for telemetry...");
        insightLabel = createBodyLabel("Topology-aware routing insights will appear here once traffic starts flowing.");
        controlStateLabel = createPillLabel();
        rateLabel = createBodyLabel("Generator rate: 10 req/sec");
        controlHintLabel = createBodyLabel("Adjust the request generator to stress-test routing and caching behavior.");
        serverSummaryLabel = createBodyLabel("No server telemetry yet.");

        startPauseButton = createActionButton("Pause Simulation", ACCENT_GREEN);
        rateSlider = createRateSlider();

        serverListPanel = new JPanel();
        serverListPanel.setOpaque(false);
        serverListPanel.setLayout(new BoxLayout(serverListPanel, BoxLayout.Y_AXIS));

        eventListPanel = new JPanel();
        eventListPanel.setOpaque(false);
        eventListPanel.setLayout(new BoxLayout(eventListPanel, BoxLayout.Y_AXIS));

        topologyPanel = new TopologyPanel();
        trendPanel = new TrendPanel();

        setContentPane(buildContent());
        wireControls();
        syncControlState(simulationController.isPaused(), simulationController.getRequestsPerSecond());
        setVisible(true);
    }

    public void update(DashboardSnapshot snapshot) {
        SwingUtilities.invokeLater(() -> applySnapshot(snapshot));
    }

    private JPanel buildContent() {
        AtmospherePanel root = new AtmospherePanel();
        root.setLayout(new BorderLayout(0, 20));
        root.setBorder(new EmptyBorder(22, 22, 22, 22));

        root.add(buildTopSection(), BorderLayout.NORTH);
        root.add(buildMainSection(), BorderLayout.CENTER);

        return root;
    }

    private JPanel buildTopSection() {
        JPanel section = new JPanel(new BorderLayout(0, 18));
        section.setOpaque(false);

        JPanel topBand = new JPanel(new GridBagLayout());
        topBand.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 18);
        gbc.weightx = 0.67;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        topBand.add(buildHeroPanel(), gbc);

        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weightx = 0.33;
        gbc.gridx = 1;
        topBand.add(buildControlPanel(), gbc);

        JPanel cardStrip = new JPanel(new GridLayout(1, 5, 14, 0));
        cardStrip.setOpaque(false);
        cardStrip.add(queueCard);
        cardStrip.add(requestCard);
        cardStrip.add(throughputCard);
        cardStrip.add(ratioCard);
        cardStrip.add(latencyCard);

        section.add(topBand, BorderLayout.NORTH);
        section.add(cardStrip, BorderLayout.CENTER);

        return section;
    }

    private Component buildHeroPanel() {
        GlassPanel hero = new GlassPanel();
        hero.setLayout(new BorderLayout(20, 0));
        hero.setBorder(new EmptyBorder(24, 26, 24, 26));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel eyebrow = createMetaLabel("AUTONOMOUS SYSTEM CONTROL");
        JLabel title = new JLabel("Distributed Cache Load Balancer");
        title.setForeground(TEXT_PRIMARY);
        title.setFont(new Font("Bahnschrift", Font.BOLD, 31));

        JLabel subtitle = createBodyLabel(
                "<html>Live observability for queue pressure, cache efficiency, server health, and routing behavior.</html>");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        left.add(eyebrow);
        left.add(Box.createVerticalStrut(10));
        left.add(title);
        left.add(Box.createVerticalStrut(8));
        left.add(subtitle);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        liveStatusLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        updatedAtLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        insightLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        right.add(liveStatusLabel);
        right.add(Box.createVerticalStrut(14));
        right.add(updatedAtLabel);
        right.add(Box.createVerticalStrut(10));
        right.add(insightLabel);

        hero.add(left, BorderLayout.CENTER);
        hero.add(right, BorderLayout.EAST);

        return hero;
    }

    private Component buildControlPanel() {
        GlassPanel controlPanel = new GlassPanel();
        controlPanel.setLayout(new BorderLayout(0, 16));
        controlPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = createSectionTitle("Control Deck");
        JLabel subtitle = createBodyLabel("Start or pause the generator and tune the request load in real time.");

        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitle);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JPanel actionRow = new JPanel(new BorderLayout(10, 0));
        actionRow.setOpaque(false);
        actionRow.add(startPauseButton, BorderLayout.WEST);
        actionRow.add(controlStateLabel, BorderLayout.EAST);

        JPanel sliderPanel = new JPanel();
        sliderPanel.setOpaque(false);
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
        sliderPanel.add(createMetaLabel("REQUEST GENERATOR RATE"));
        sliderPanel.add(Box.createVerticalStrut(10));
        sliderPanel.add(rateSlider);
        sliderPanel.add(Box.createVerticalStrut(10));
        sliderPanel.add(rateLabel);

        body.add(actionRow);
        body.add(Box.createVerticalStrut(18));
        body.add(sliderPanel);
        body.add(Box.createVerticalStrut(12));
        body.add(controlHintLabel);

        controlPanel.add(header, BorderLayout.NORTH);
        controlPanel.add(body, BorderLayout.CENTER);

        return controlPanel;
    }

    private JPanel buildMainSection() {
        JPanel main = new JPanel(new GridBagLayout());
        main.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 18, 18);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.58;
        gbc.weighty = 0.52;
        gbc.gridx = 0;
        gbc.gridy = 0;
        main.add(buildSectionCard(
                "Topology Intelligence",
                "A cleaner network view with click-to-open managed-server details.",
                topologyPanel), gbc);

        gbc.weightx = 0.42;
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 18, 0);
        main.add(buildServersCard(), gbc);

        gbc.weightx = 0.58;
        gbc.weighty = 0.48;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 18);
        main.add(buildSectionCard(
                "Flow Trends",
                "Recent queue, throughput, and cache-efficiency movement.",
                trendPanel), gbc);

        gbc.weightx = 0.42;
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        main.add(buildEventsCard(), gbc);

        return main;
    }

    private Component buildServersCard() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);

        body.add(serverSummaryLabel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(serverListPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);

        body.add(scrollPane, BorderLayout.CENTER);

        GlassPanel card = buildSectionCard(
                "Managed Servers",
                "Compact live cards with active load, cache behavior, and detailed popups.",
                body);
        card.setPreferredSize(new Dimension(470, 360));
        return card;
    }

    private Component buildEventsCard() {
        JScrollPane scrollPane = new JScrollPane(eventListPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);

        GlassPanel card = buildSectionCard(
                "Activity Feed",
                "High-signal simulation, cache, routing, and recovery events.",
                scrollPane);
        card.setPreferredSize(new Dimension(470, 320));
        return card;
    }

    private GlassPanel buildSectionCard(String title, String subtitle, Component body) {
        GlassPanel card = new GlassPanel();
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = createSectionTitle(title);
        JLabel subtitleLabel = createBodyLabel("<html>" + subtitle + "</html>");

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitleLabel);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        return card;
    }

    private void wireControls() {
        startPauseButton.addActionListener(e -> {
            simulationController.toggleSimulation();
            syncControlState(simulationController.isPaused(), simulationController.getRequestsPerSecond());
        });

        rateSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int rate = rateSlider.getValue();
                rateLabel.setText("Generator rate: " + rate + " req/sec");

                if (!rateSlider.getValueIsAdjusting()) {
                    simulationController.setRequestsPerSecond(rate);
                    syncControlState(simulationController.isPaused(), rate);
                }
            }
        });
    }

    private void applySnapshot(DashboardSnapshot snapshot) {
        latestSnapshot = snapshot;

        int throughput = Math.max(0, snapshot.getTotalRequests() - lastTotalRequests);
        lastTotalRequests = snapshot.getTotalRequests();

        pushHistory(queueHistory, snapshot.getQueueSize());
        pushHistory(throughputHistory, throughput);
        pushHistory(hitRatioHistory, snapshot.getHitRatio());

        queueCard.setMetric(Integer.toString(snapshot.getQueueSize()), queueInsight(snapshot.getQueueSize()));
        requestCard.setMetric(
                Integer.toString(snapshot.getTotalRequests()),
                "Hits " + snapshot.getCacheHits() + " | Misses " + snapshot.getCacheMisses());
        throughputCard.setMetric(throughput + " / sec", throughputInsight(throughput));
        ratioCard.setMetric(numberFormat.format(snapshot.getHitRatio()) + "%", ratioInsight(snapshot.getHitRatio()));
        latencyCard.setMetric(numberFormat.format(snapshot.getAverageLatency()) + " ms",
                latencyInsight(snapshot.getAverageLatency()));

        liveStatusLabel.setText(systemStatus(snapshot));
        liveStatusLabel.setBackground(systemStatusColor(snapshot));
        updatedAtLabel.setText("Last refresh: " + timeFormat.format(new Date(snapshot.getCapturedAtMillis())));
        insightLabel.setText("<html>" + buildHeroInsight(snapshot, throughput) + "</html>");

        syncControlState(snapshot.isSimulationPaused(), snapshot.getConfiguredRatePerSecond());

        refreshServerPanel(snapshot.getServers());
        refreshEventPanel(snapshot.getRecentEvents());

        topologyPanel.updateServers(snapshot.getServers());
        trendPanel.updateData(queueHistory, throughputHistory, hitRatioHistory);
    }

    private void syncControlState(boolean paused, int configuredRate) {
        startPauseButton.setText(paused ? "Resume Simulation" : "Pause Simulation");
        startPauseButton.setBackground(paused ? new Color(34, 88, 141) : new Color(18, 117, 82));
        controlStateLabel.setText(paused ? "SIMULATION PAUSED" : "SIMULATION LIVE");
        controlStateLabel.setBackground(paused ? new Color(117, 45, 58) : new Color(24, 102, 77));
        controlHintLabel.setText(paused
                ? "Traffic generation is paused. The workers will continue draining any queued requests."
                : "Traffic generation is live. Use the slider to control how aggressively the queue is filled.");
        if (rateSlider.getValue() != configuredRate) {
            rateSlider.setValue(configuredRate);
        }
        rateLabel.setText("Generator rate: " + configuredRate + " req/sec");
    }

    private void refreshServerPanel(List<ServerSnapshot> servers) {
        serverListPanel.removeAll();

        int activeCount = 0;
        int maxLoad = 1;
        int busiestProcessed = 1;

        for (ServerSnapshot snapshot : servers) {
            maxLoad = Math.max(maxLoad, snapshot.getActiveLoad());
            busiestProcessed = Math.max(busiestProcessed, snapshot.getProcessedRequests());

            if (snapshot.isActive()) {
                activeCount++;
            }
        }

        serverSummaryLabel.setText(activeCount + " / " + servers.size()
                + " servers healthy | detailed metrics available on click.");

        for (ServerSnapshot snapshot : servers) {
            serverListPanel.add(createServerTile(snapshot, maxLoad, busiestProcessed));
            serverListPanel.add(Box.createVerticalStrut(10));
        }

        serverListPanel.revalidate();
        serverListPanel.repaint();
    }

    private JPanel createServerTile(ServerSnapshot snapshot, int maxLoad, int busiestProcessed) {
        GlassPanel tile = new GlassPanel();
        tile.setLayout(new BorderLayout(16, 0));
        tile.setBorder(new EmptyBorder(16, 16, 16, 16));
        tile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel name = new JLabel("Server " + snapshot.getServerId());
        name.setForeground(TEXT_PRIMARY);
        name.setFont(new Font("Bahnschrift", Font.BOLD, 21));

        JLabel detail = createBodyLabel("Processed: " + snapshot.getProcessedRequests()
                + " | Hit ratio: " + numberFormat.format(snapshot.getCacheHitRatio()) + "%");

        JPanel chipRow = new JPanel();
        chipRow.setOpaque(false);
        chipRow.setLayout(new BoxLayout(chipRow, BoxLayout.X_AXIS));

        chipRow.add(createSmallChip(snapshot.isActive() ? "ACTIVE" : "RECOVERING",
                snapshot.isActive() ? new Color(24, 102, 77) : new Color(117, 45, 58)));
        chipRow.add(Box.createHorizontalStrut(8));
        chipRow.add(createSmallChip("CACHE " + snapshot.getCacheOccupancy() + "/" + snapshot.getCacheCapacity(),
                new Color(35, 74, 124)));
        chipRow.add(Box.createHorizontalStrut(8));
        chipRow.add(createSmallChip("LAT " + snapshot.getLastLatencyMillis() + " ms", new Color(92, 88, 31)));

        left.add(name);
        left.add(Box.createVerticalStrut(4));
        left.add(detail);
        left.add(Box.createVerticalStrut(10));
        left.add(chipRow);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        right.add(createMetaLabel("ACTIVE LOAD"));
        right.add(Box.createVerticalStrut(8));
        LoadBar loadBar = new LoadBar(snapshot.getActiveLoad(), Math.max(1, maxLoad), snapshot.isActive(), ACCENT_CYAN);
        loadBar.setPreferredSize(new Dimension(180, 16));
        right.add(loadBar);
        right.add(Box.createVerticalStrut(6));
        right.add(createMiniValue(snapshot.getActiveLoad() + " in-flight"));
        right.add(Box.createVerticalStrut(12));
        right.add(createMetaLabel("WORK SHARE"));
        right.add(Box.createVerticalStrut(8));
        LoadBar processedBar = new LoadBar(
                snapshot.getProcessedRequests(),
                Math.max(1, busiestProcessed),
                true,
                ACCENT_ORANGE);
        processedBar.setPreferredSize(new Dimension(180, 16));
        right.add(processedBar);
        right.add(Box.createVerticalStrut(6));
        right.add(createMiniValue(numberFormat.format(percentage(
                snapshot.getProcessedRequests(),
                Math.max(1, busiestProcessed))) + "% of busiest"));

        MouseAdapter openDetailsListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showServerDetails(snapshot);
            }
        };

        tile.addMouseListener(openDetailsListener);
        left.addMouseListener(openDetailsListener);
        right.addMouseListener(openDetailsListener);

        JButton detailsButton = createSecondaryButton("View Details");
        detailsButton.addActionListener(e -> showServerDetails(snapshot));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(detailsButton, BorderLayout.EAST);

        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);
        container.add(tile, BorderLayout.CENTER);
        tile.add(left, BorderLayout.CENTER);
        tile.add(right, BorderLayout.EAST);
        container.add(footer, BorderLayout.SOUTH);

        return container;
    }

    private void refreshEventPanel(List<SystemEvent> events) {
        eventListPanel.removeAll();

        if (events.isEmpty()) {
            eventListPanel.add(createEmptyMessage("Waiting for system activity..."));
        } else {
            int limit = Math.min(events.size(), 10);

            for (int i = 0; i < limit; i++) {
                eventListPanel.add(createEventRow(events.get(i)));
                eventListPanel.add(Box.createVerticalStrut(10));
            }
        }

        eventListPanel.revalidate();
        eventListPanel.repaint();
    }

    private JPanel createEventRow(SystemEvent event) {
        GlassPanel row = new GlassPanel();
        row.setLayout(new BorderLayout(12, 0));
        row.setBorder(new EmptyBorder(14, 14, 14, 14));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));

        JLabel time = createMetaLabel(event.getTime());
        time.setForeground(TEXT_MUTED);
        time.setPreferredSize(new Dimension(72, 24));

        JPanel middle = new JPanel();
        middle.setOpaque(false);
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));

        JLabel type = createSmallChip(formatEventType(event.getType()), eventColor(event.getType()));
        JLabel message = createBodyLabel("<html>" + event.getMessage() + "</html>");
        message.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        middle.add(type);
        middle.add(Box.createVerticalStrut(8));
        middle.add(message);

        row.add(time, BorderLayout.WEST);
        row.add(middle, BorderLayout.CENTER);

        return row;
    }

    private void showServerDetails(ServerSnapshot snapshot) {
        JDialog dialog = new JDialog(this, "Server " + snapshot.getServerId() + " Details", false);
        dialog.setSize(520, 460);
        dialog.setLocationRelativeTo(this);

        AtmospherePanel root = new AtmospherePanel();
        root.setLayout(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(18, 18, 18, 18));

        GlassPanel hero = new GlassPanel();
        hero.setLayout(new BorderLayout(10, 0));
        hero.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Server " + snapshot.getServerId());
        title.setForeground(TEXT_PRIMARY);
        title.setFont(new Font("Bahnschrift", Font.BOLD, 26));
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(8));
        titleBlock.add(createBodyLabel("Per-server load, cache, and latency breakdown."));

        JPanel statusBlock = new JPanel();
        statusBlock.setOpaque(false);
        statusBlock.setLayout(new BoxLayout(statusBlock, BoxLayout.Y_AXIS));
        statusBlock.add(createSmallChip(snapshot.isActive() ? "ACTIVE" : "RECOVERING",
                snapshot.isActive() ? new Color(24, 102, 77) : new Color(117, 45, 58)));
        statusBlock.add(Box.createVerticalStrut(10));
        statusBlock.add(createSmallChip("CACHE " + snapshot.getCacheOccupancy() + "/" + snapshot.getCacheCapacity(),
                new Color(35, 74, 124)));

        hero.add(titleBlock, BorderLayout.CENTER);
        hero.add(statusBlock, BorderLayout.EAST);

        GlassPanel metricsPanel = new GlassPanel();
        metricsPanel.setBorder(new EmptyBorder(18, 18, 18, 18));
        metricsPanel.setLayout(new GridLayout(3, 2, 12, 12));
        metricsPanel.add(createPopupStat("Active Load", snapshot.getActiveLoad() + " in-flight"));
        metricsPanel.add(createPopupStat("Processed Requests", Integer.toString(snapshot.getProcessedRequests())));
        metricsPanel.add(createPopupStat("Cache Hits", Integer.toString(snapshot.getCacheHits())));
        metricsPanel.add(createPopupStat("Cache Misses", Integer.toString(snapshot.getCacheMisses())));
        metricsPanel.add(createPopupStat("Cache Hit Ratio", numberFormat.format(snapshot.getCacheHitRatio()) + "%"));
        metricsPanel.add(createPopupStat("Last Latency", snapshot.getLastLatencyMillis() + " ms"));

        GlassPanel relatedPanel = new GlassPanel();
        relatedPanel.setLayout(new BorderLayout(0, 10));
        relatedPanel.setBorder(new EmptyBorder(18, 18, 18, 18));

        JLabel relatedTitle = createSectionTitle("Recent Related Events");
        relatedTitle.setFont(new Font("Bahnschrift", Font.BOLD, 18));
        relatedPanel.add(relatedTitle, BorderLayout.NORTH);

        JPanel eventsPanel = new JPanel();
        eventsPanel.setOpaque(false);
        eventsPanel.setLayout(new BoxLayout(eventsPanel, BoxLayout.Y_AXIS));

        List<SystemEvent> relatedEvents = findRelatedEvents(snapshot.getServerId());
        if (relatedEvents.isEmpty()) {
            eventsPanel.add(createEmptyMessage("No recent server-specific events captured yet."));
        } else {
            for (SystemEvent event : relatedEvents) {
                JLabel eventLabel = createBodyLabel("<html><b>" + event.getTime() + "</b>  " + event.getMessage() + "</html>");
                eventLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
                eventsPanel.add(eventLabel);
            }
        }

        JScrollPane eventScroll = new JScrollPane(eventsPanel);
        eventScroll.setOpaque(false);
        eventScroll.getViewport().setOpaque(false);
        eventScroll.setBorder(BorderFactory.createEmptyBorder());
        relatedPanel.add(eventScroll, BorderLayout.CENTER);

        root.add(hero, BorderLayout.NORTH);
        root.add(metricsPanel, BorderLayout.CENTER);
        root.add(relatedPanel, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private List<SystemEvent> findRelatedEvents(int serverId) {
        List<SystemEvent> related = new ArrayList<>();

        if (latestSnapshot == null) {
            return related;
        }

        String marker = "Server " + serverId;
        for (SystemEvent event : latestSnapshot.getRecentEvents()) {
            if (event.getMessage().contains(marker)) {
                related.add(event);
            }

            if (related.size() == 6) {
                break;
            }
        }

        return related;
    }

    private JPanel createPopupStat(String title, String value) {
        GlassPanel panel = new GlassPanel();
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel titleLabel = createMetaLabel(title.toUpperCase(Locale.ENGLISH));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(TEXT_PRIMARY);
        valueLabel.setFont(new Font("Bahnschrift", Font.BOLD, 20));

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(valueLabel);

        return panel;
    }

    private JLabel createEmptyMessage(String text) {
        JLabel label = createBodyLabel(text);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(new EmptyBorder(18, 0, 18, 0));
        return label;
    }

    private JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_PRIMARY);
        label.setFont(new Font("Bahnschrift", Font.BOLD, 22));
        return label;
    }

    private JLabel createMetaLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ACCENT_CYAN);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        return label;
    }

    private JLabel createBodyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_SECONDARY);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return label;
    }

    private JLabel createMiniValue(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_PRIMARY);
        label.setFont(new Font("Bahnschrift", Font.BOLD, 15));
        return label;
    }

    private JLabel createPillLabel() {
        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setOpaque(true);
        label.setForeground(TEXT_PRIMARY);
        label.setBackground(new Color(22, 92, 118));
        label.setBorder(new EmptyBorder(7, 12, 7, 12));
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        return label;
    }

    private JLabel createSmallChip(String text, Color background) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setForeground(TEXT_PRIMARY);
        label.setBackground(background);
        label.setBorder(new EmptyBorder(5, 10, 5, 10));
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
        return label;
    }

    private JButton createActionButton(String text, Color background) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(background);
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(new Color(26, 63, 96));
        button.setBorder(new EmptyBorder(8, 12, 8, 12));
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JSlider createRateSlider() {
        JSlider slider = new JSlider(1, 25, 10);
        slider.setOpaque(false);
        slider.setPaintTicks(true);
        slider.setPaintLabels(false);
        slider.setMajorTickSpacing(4);
        slider.setMinorTickSpacing(1);
        slider.setForeground(ACCENT_CYAN);
        return slider;
    }

    private String queueInsight(int queueSize) {
        if (queueSize == 0) {
            return "Pipeline is clear and ready.";
        }
        if (queueSize < 8) {
            return "Backpressure is under control.";
        }
        if (queueSize < 20) {
            return "Traffic is rising, but stable.";
        }
        return "Backlog is building quickly.";
    }

    private String throughputInsight(int throughput) {
        if (throughput == 0) {
            return "System is still warming up.";
        }
        if (throughput < 5) {
            return "Measured and steady flow.";
        }
        if (throughput < 15) {
            return "Healthy processing rhythm.";
        }
        return "High processing intensity detected.";
    }

    private String ratioInsight(double hitRatio) {
        if (hitRatio < 30) {
            return "Cache locality is still maturing.";
        }
        if (hitRatio < 60) {
            return "Cache is helping, with headroom left.";
        }
        if (hitRatio < 80) {
            return "Strong cache performance.";
        }
        return "Excellent cache reuse across traffic.";
    }

    private String latencyInsight(double latency) {
        if (latency < 20) {
            return "Very low average latency.";
        }
        if (latency < 60) {
            return "Latency is comfortably controlled.";
        }
        if (latency < 100) {
            return "Latency is acceptable but watchable.";
        }
        return "Latency pressure is becoming visible.";
    }

    private String buildHeroInsight(DashboardSnapshot snapshot, int throughput) {
        int activeServers = 0;

        for (ServerSnapshot server : snapshot.getServers()) {
            if (server.isActive()) {
                activeServers++;
            }
        }

        String simulationMode = snapshot.isSimulationPaused() ? "Generator paused" : "Generator live";
        return activeServers + "/" + snapshot.getServers().size()
               + " servers healthy | "
               + throughput + "/sec throughput | "
               + numberFormat.format(snapshot.getHitRatio()) + "% cache efficiency | "
               + simulationMode + " at " + snapshot.getConfiguredRatePerSecond() + " req/sec";
    }

    private String systemStatus(DashboardSnapshot snapshot) {
        boolean degraded = snapshot.getQueueSize() > 20 || snapshot.getAverageLatency() > 100;

        for (ServerSnapshot server : snapshot.getServers()) {
            if (!server.isActive()) {
                degraded = true;
                break;
            }
        }

        if (snapshot.isSimulationPaused()) {
            return "PAUSED MONITORING";
        }
        if (degraded) {
            return "WATCH SYSTEM";
        }
        if (snapshot.getHitRatio() > 65 && snapshot.getQueueSize() < 10) {
            return "OPTIMAL FLOW";
        }
        return "STABLE ROUTING";
    }

    private Color systemStatusColor(DashboardSnapshot snapshot) {
        String status = systemStatus(snapshot);

        if ("WATCH SYSTEM".equals(status)) {
            return new Color(117, 45, 58);
        }
        if ("OPTIMAL FLOW".equals(status)) {
            return new Color(24, 102, 77);
        }
        if ("PAUSED MONITORING".equals(status)) {
            return new Color(84, 76, 29);
        }
        return new Color(22, 92, 118);
    }

    private String formatEventType(String type) {
        return type.replace('_', ' ');
    }

    private Color eventColor(String type) {
        if ("CACHE_HIT".equals(type)) {
            return new Color(24, 102, 77);
        }
        if ("CACHE_MISS".equals(type)) {
            return new Color(128, 87, 37);
        }
        if ("SERVER".equals(type)) {
            return new Color(117, 45, 58);
        }
        if ("ROUTE".equals(type)) {
            return new Color(22, 92, 118);
        }
        if ("REQUEST".equals(type)) {
            return new Color(37, 74, 124);
        }
        return new Color(75, 79, 109);
    }

    private double percentage(int value, int max) {
        return max == 0 ? 0 : (value * 100.0) / max;
    }

    private void pushHistory(List<Integer> history, int value) {
        if (history.size() == HISTORY_LIMIT) {
            history.remove(0);
        }
        history.add(value);
    }

    private void pushHistory(List<Double> history, double value) {
        if (history.size() == HISTORY_LIMIT) {
            history.remove(0);
        }
        history.add(value);
    }

    private static class AtmospherePanel extends JPanel {
        AtmospherePanel() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            g2.setPaint(new GradientPaint(0, 0, PAGE_TOP, 0, height / 2f, PAGE_MIDDLE));
            g2.fillRect(0, 0, width, height);
            g2.setPaint(new GradientPaint(0, height / 2f, PAGE_MIDDLE, width, height, PAGE_BOTTOM));
            g2.fillRect(0, height / 2, width, height / 2);

            g2.setColor(new Color(255, 255, 255, 9));
            for (int x = 0; x < width; x += 38) {
                g2.drawLine(x, 0, x, height);
            }
            for (int y = 0; y < height; y += 38) {
                g2.drawLine(0, y, width, y);
            }

            g2.setColor(new Color(39, 128, 180, 40));
            g2.fillOval(width - 260, -80, 220, 220);
            g2.setColor(new Color(85, 103, 255, 30));
            g2.fillOval(-70, height - 230, 260, 260);

            g2.dispose();
        }
    }

    private static class GlassPanel extends JPanel {
        GlassPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD_SHADOW);
            g2.fillRoundRect(4, 8, getWidth() - 8, getHeight() - 8, 26, 26);
            g2.setColor(CARD_FILL);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
            g2.setColor(CARD_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class MetricCard extends GlassPanel {

        private final JLabel valueLabel;
        private final JLabel insightLabel;
        private final Color accent;

        MetricCard(String title, Color accent) {
            this.accent = accent;
            setLayout(new BorderLayout(0, 12));
            setBorder(new EmptyBorder(18, 18, 18, 18));
            setPreferredSize(new Dimension(220, 138));

            JLabel titleLabel = new JLabel(title);
            titleLabel.setForeground(TEXT_SECONDARY);
            titleLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));

            valueLabel = new JLabel("--");
            valueLabel.setForeground(TEXT_PRIMARY);
            valueLabel.setFont(new Font("Bahnschrift", Font.BOLD, 28));

            insightLabel = new JLabel("Awaiting live data");
            insightLabel.setForeground(TEXT_SECONDARY);
            insightLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.add(titleLabel);
            center.add(Box.createVerticalStrut(12));
            center.add(valueLabel);
            center.add(Box.createVerticalStrut(10));
            center.add(insightLabel);

            add(center, BorderLayout.CENTER);
        }

        void setMetric(String value, String insight) {
            valueLabel.setText(value);
            insightLabel.setText("<html>" + insight + "</html>");
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(accent);
            g2.fillRoundRect(16, 12, 62, 6, 6, 6);
            g2.dispose();
        }
    }

    private static class LoadBar extends JPanel {

        private final int value;
        private final int max;
        private final boolean active;
        private final Color accent;

        LoadBar(int value, int max, boolean active, Color accent) {
            this.value = value;
            this.max = Math.max(1, max);
            this.active = active;
            this.accent = accent;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int filled = value == 0 ? 0 : Math.max(8, (int) Math.round(((value * 1.0) / max) * width));

            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillRoundRect(0, 0, width, height, height, height);

            Color endColor = active ? ACCENT_GREEN : new Color(191, 76, 89);
            g2.setPaint(new GradientPaint(0, 0, accent, width, 0, endColor));
            g2.fillRoundRect(0, 0, Math.min(width, filled), height, height, height);

            g2.dispose();
        }
    }

    private class TopologyPanel extends JPanel {

        private final Map<Integer, ServerSnapshot> serversById = new HashMap<>();
        private final Map<Integer, Ellipse2D.Double> nodeBounds = new HashMap<>();

        TopologyPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(760, 310));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Integer clickedNode = findNodeAt(e.getX(), e.getY());
                    if (clickedNode != null) {
                        ServerSnapshot snapshot = serversById.get(clickedNode);
                        if (snapshot != null) {
                            showServerDetails(snapshot);
                        }
                    }
                }
            });
        }

        void updateServers(List<ServerSnapshot> snapshots) {
            serversById.clear();
            for (ServerSnapshot snapshot : snapshots) {
                serversById.put(snapshot.getServerId(), snapshot);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            Map<Integer, int[]> positions = buildTopologyPositions(width, height);
            nodeBounds.clear();

            g2.setColor(new Color(255, 255, 255, 14));
            for (int x = 30; x < width - 20; x += 90) {
                g2.drawLine(x, 28, x, height - 30);
            }

            g2.setStroke(new BasicStroke(2f));
            for (int[] connection : networkConnections) {
                int[] from = positions.get(connection[0]);
                int[] to = positions.get(connection[1]);

                if (from == null || to == null) {
                    continue;
                }

                g2.setColor(new Color(115, 168, 205, 72));
                g2.drawLine(from[0], from[1], to[0], to[1]);

                int labelX = (from[0] + to[0]) / 2;
                int labelY = (from[1] + to[1]) / 2;
                g2.setColor(new Color(209, 228, 246, 120));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.drawString(String.valueOf(connection[2]), labelX, labelY);
            }

            for (Integer nodeId : networkNodes) {
                int[] point = positions.get(nodeId);
                ServerSnapshot server = serversById.get(nodeId);

                boolean managed = server != null;
                boolean active = managed && server.isActive();
                int size = managed ? 28 + Math.min(server.getProcessedRequests() / 18, 12) : 22;

                Color fill = new Color(29, 63, 92);
                if (managed && active) {
                    fill = new Color(34, 168, 163);
                } else if (managed) {
                    fill = new Color(191, 76, 89);
                }

                int outer = size + 12;
                g2.setColor(new Color(255, 255, 255, managed ? 55 : 16));
                g2.fillOval(point[0] - outer / 2, point[1] - outer / 2, outer, outer);
                g2.setColor(fill);
                g2.fillOval(point[0] - size / 2, point[1] - size / 2, size, size);

                g2.setColor(TEXT_PRIMARY);
                g2.setFont(new Font("Bahnschrift", Font.BOLD, 15));
                g2.drawString(String.valueOf(nodeId), point[0] - 5, point[1] + 5);

                if (managed) {
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    g2.setColor(TEXT_SECONDARY);
                    g2.drawString(active ? server.getActiveLoad() + " active" : "recovering",
                            point[0] - 24,
                            point[1] + size / 2 + 16);
                }

                nodeBounds.put(nodeId, new Ellipse2D.Double(point[0] - size / 2.0, point[1] - size / 2.0, size, size));
            }

            g2.setColor(TEXT_MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString("Managed nodes glow brighter and can be clicked for deeper diagnostics.", 20, height - 10);

            g2.dispose();
        }

        private Map<Integer, int[]> buildTopologyPositions(int width, int height) {
            double[][] normalized = {
                    {0.10, 0.48},
                    {0.20, 0.24},
                    {0.31, 0.47},
                    {0.43, 0.20},
                    {0.52, 0.66},
                    {0.66, 0.46},
                    {0.77, 0.22},
                    {0.82, 0.72},
                    {0.92, 0.44},
                    {0.60, 0.10}
            };

            Map<Integer, int[]> positions = new HashMap<>();
            int usableWidth = width - 120;
            int usableHeight = height - 90;

            for (int i = 0; i < networkNodes.size(); i++) {
                int nodeId = networkNodes.get(i);
                double[] point = normalized[Math.min(i, normalized.length - 1)];
                int x = 60 + (int) Math.round(point[0] * usableWidth);
                int y = 32 + (int) Math.round(point[1] * usableHeight);
                positions.put(nodeId, new int[] { x, y });
            }

            return positions;
        }

        private Integer findNodeAt(int x, int y) {
            for (Map.Entry<Integer, Ellipse2D.Double> entry : nodeBounds.entrySet()) {
                if (entry.getValue().contains(x, y)) {
                    return entry.getKey();
                }
            }
            return null;
        }
    }

    private static class TrendPanel extends JPanel {

        private List<Integer> queueHistory = new ArrayList<>();
        private List<Integer> throughputHistory = new ArrayList<>();
        private List<Double> ratioHistory = new ArrayList<>();

        TrendPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(760, 290));
        }

        void updateData(List<Integer> queueHistory, List<Integer> throughputHistory, List<Double> ratioHistory) {
            this.queueHistory = new ArrayList<>(queueHistory);
            this.throughputHistory = new ArrayList<>(throughputHistory);
            this.ratioHistory = new ArrayList<>(ratioHistory);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int left = 20;
            int right = width - 20;
            int top = 18;
            int bottom = height - 44;

            g2.setColor(new Color(255, 255, 255, 16));
            for (int i = 0; i < 5; i++) {
                int y = top + ((bottom - top) * i / 4);
                g2.drawLine(left, y, right, y);
            }

            int maxValue = maxValue(queueHistory, throughputHistory);
            drawSeries(g2, queueHistory, left, right, top, bottom, ACCENT_CYAN, maxValue);
            drawSeries(g2, throughputHistory, left, right, top, bottom, ACCENT_ORANGE, maxValue);
            drawRatioSeries(g2, ratioHistory, left, right, top, bottom, ACCENT_GREEN);

            g2.setColor(TEXT_MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.drawString("0", 4, bottom + 4);
            g2.drawString(String.valueOf(maxValue), 2, top + 4);
            g2.drawString("100%", right - 34, top + 4);

            drawLegend(g2, left, height - 18);

            g2.dispose();
        }

        private void drawLegend(Graphics2D g2, int left, int baseline) {
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(TEXT_SECONDARY);
            g2.drawString("Queue", left, baseline);
            g2.setColor(ACCENT_CYAN);
            g2.fillRoundRect(left + 44, baseline - 7, 18, 6, 6, 6);

            g2.setColor(TEXT_SECONDARY);
            g2.drawString("Throughput", left + 84, baseline);
            g2.setColor(ACCENT_ORANGE);
            g2.fillRoundRect(left + 156, baseline - 7, 18, 6, 6, 6);

            g2.setColor(TEXT_SECONDARY);
            g2.drawString("Hit Ratio", left + 196, baseline);
            g2.setColor(ACCENT_GREEN);
            g2.fillRoundRect(left + 258, baseline - 7, 18, 6, 6, 6);
        }

        private void drawSeries(
                Graphics2D g2,
                List<Integer> series,
                int left,
                int right,
                int top,
                int bottom,
                Color color,
                int maxValue) {

            if (series.size() < 2 || maxValue <= 0) {
                return;
            }

            Path2D path = new Path2D.Double();
            int chartWidth = right - left;
            int chartHeight = bottom - top;

            for (int i = 0; i < series.size(); i++) {
                double x = left + (chartWidth * i / (double) (series.size() - 1));
                double normalized = series.get(i) / (double) maxValue;
                double y = bottom - (normalized * chartHeight);

                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }

            g2.setStroke(new BasicStroke(2.5f));
            g2.setColor(color);
            g2.draw(path);
        }

        private void drawRatioSeries(
                Graphics2D g2,
                List<Double> series,
                int left,
                int right,
                int top,
                int bottom,
                Color color) {

            if (series.size() < 2) {
                return;
            }

            Path2D path = new Path2D.Double();
            int chartWidth = right - left;
            int chartHeight = bottom - top;

            for (int i = 0; i < series.size(); i++) {
                double x = left + (chartWidth * i / (double) (series.size() - 1));
                double normalized = Math.max(0, Math.min(100, series.get(i))) / 100.0;
                double y = bottom - (normalized * chartHeight);

                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }

            g2.setStroke(new BasicStroke(2f));
            g2.setColor(color);
            g2.draw(path);
        }

        private int maxValue(List<Integer> queueHistory, List<Integer> throughputHistory) {
            int max = 1;

            for (Integer value : queueHistory) {
                max = Math.max(max, value);
            }

            for (Integer value : throughputHistory) {
                max = Math.max(max, value);
            }

            return max;
        }
    }
}
